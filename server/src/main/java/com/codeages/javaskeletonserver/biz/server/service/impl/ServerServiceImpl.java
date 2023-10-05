package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.dto.*;
import com.codeages.javaskeletonserver.biz.server.entity.Server;
import com.codeages.javaskeletonserver.biz.server.mapper.ServerMapper;
import com.codeages.javaskeletonserver.biz.server.repository.ServerRepository;
import com.codeages.javaskeletonserver.biz.server.service.ProxyService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.util.TreeUtils;
import com.codeages.javaskeletonserver.exception.AppException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;

    private final ServerMapper serverMapper;

    private final Validator validator;

    private final ProxyService proxyService;

    @Value("${webssh.url}")
    private String websshUrl;

    public ServerServiceImpl(ServerRepository serverRepository, ServerMapper serverMapper, Validator validator,
                             ProxyService proxyService) {
        this.serverRepository = serverRepository;
        this.serverMapper = serverMapper;
        this.validator = validator;
        this.proxyService = proxyService;
    }

    @Override
    public void update(ServerUpdateParams params) {
        var errors = validator.validate(params);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        Server server = serverRepository.findById(params.getId())
                                        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        serverMapper.toUpdateEntity(server, params);
        serverRepository.save(server);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void sort(List<TreeSortParams> treeSortParams) {
        for (int i = 0; i < treeSortParams.size(); i++) {
            treeSortParams.get(i).setSort((long) i);
            TreeUtils.rebuildSeq(treeSortParams.get(i));
        }

        serverRepository.saveAll(toUpdateAllEntity(treeSortParams));
    }

    @Override
    public ServerDto findById(Long id) {
        ServerDto serverDto = serverRepository.findById(id)
                                              .map(serverMapper::toDto)
                                              .orElseThrow(() -> new AppException(
                                                      ErrorCode.INVALID_ARGUMENT,
                                                      "服务器不存在"
                                              ));

        Long proxyId = serverDto.getProxyId();
        while (proxyId == null) {
            if (serverDto.getParentId() == null || serverDto.getParentId() == 0) {
                break;
            }

            proxyId = serverRepository.findById(serverDto.getParentId())
                                      .map(serverMapper::toDto)
                                      .orElseThrow(() -> new AppException(
                                              ErrorCode.INVALID_ARGUMENT,
                                              "服务器不存在"
                                      )).getProxyId();
        }

        if (proxyId != null) {
            serverDto.setProxy(proxyService.findById(proxyId)
                                           .orElseThrow(() -> new AppException(
                                                   ErrorCode.INVALID_ARGUMENT,
                                                   "代理不存在"
                                           )));
        }

        return serverDto;
    }

    private List<Server> toUpdateAllEntity(List<TreeSortParams> serverUpdateParams) {
        if (CollectionUtil.isEmpty(serverUpdateParams)) {
            return Collections.emptyList();
        }

        List<Server> servers = new ArrayList<>();

        for (TreeSortParams serverUpdateParam : serverUpdateParams) {
            servers.add(
                    serverMapper.toUpdateAllEntity(
                            serverRepository.findById(serverUpdateParam.getId())
                                            .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)),
                            serverUpdateParam
                    )
            );
            servers.addAll(toUpdateAllEntity(serverUpdateParam.getChildren()));
        }

        return servers;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        List<Server> children = serverRepository.findAllByParentId(id);
        if (CollectionUtil.isNotEmpty(children)) {
            children.stream().map(Server::getId).forEach(this::delete);
        }

        Server server = serverRepository.findById(id)
                                        .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        serverRepository.delete(server);
    }

    @Override
    public void create(ServerCreateParams params) {
        Set<ConstraintViolation<ServerCreateParams>> errors = validator.validate(params);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        Server server = serverMapper.toCreateEntity(params);

        Optional<Server> maxSeqServer = serverRepository.getFirstByParentIdOrderBySortDesc(params.getParentId());
        server.setSort(maxSeqServer.map(Server::getSort).orElse(0L) + 1);

        serverRepository.save(server);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tree<Long>> findAll() {
        List<ProxyDto> proxyDtoList = proxyService.search(new ProxySearchParams(), Pageable.unpaged()).getContent();
        Map<Long, ProxyDto> proxyIdProxyMap = proxyDtoList.stream()
                                                          .collect(Collectors.toMap(
                                                                  ProxyDto::getId,
                                                                  Function.identity()
                                                          ));

        Base64.Encoder encoder = Base64.getEncoder();
        List<TreeNode<Long>> servers = serverRepository.findAll(Sort.by("sort"))
                                                       .stream()
                                                       .map(e -> {
                                                           TreeNode<Long> longTreeNode = new TreeNode<>(
                                                                   e.getId(),
                                                                   e.getParentId(),
                                                                   e.getName(),
                                                                   e.getSort()
                                                           );

                                                           longTreeNode.setExtra(BeanUtil.beanToMap(e));
                                                           String url = websshUrl +
                                                                   "?hostname=" + e.getIp() +
                                                                   "&username=" + e.getUsername() +
                                                                   "&port=" + e.getPort() +
                                                                   "&password=" + encoder.encodeToString(e.getPassword()
                                                                                                          .getBytes());

                                                           longTreeNode.getExtra().put("url", url);
                                                           return longTreeNode;
                                                       })
                                                       .collect(Collectors.toList());

        List<Tree<Long>> treeList = TreeUtil.build(servers, 0L);

        buildProxy(treeList, proxyIdProxyMap, null);

        return treeList;
    }

    /**
     * 递归构建每个节点的代理，子节点的代理会覆盖父节点的代理，取就近原则
     */
    private void buildProxy(List<Tree<Long>> servers, Map<Long, ProxyDto> proxyIdProxyMap, Long parentProxyId) {
        for (Tree<Long> serverTree : servers) {
            //如果是组节点，递归构建子节点的代理
            if (Boolean.TRUE.equals(serverTree.get("isGroup"))) {
                if (serverTree.hasChild()) {
                    Long proxyId = (Long) serverTree.get("proxyId");
                    buildProxy(serverTree.getChildren(), proxyIdProxyMap, proxyId == null ? parentProxyId : proxyId);
                }
                continue;
            }

            //如果是服务器节点，构建代理
            Long proxyId = (Long) serverTree.get("proxyId");
            //如果节点没有代理，就用父节点的代理
            if (proxyId == null) {
                proxyId = parentProxyId;
            }

            ProxyDto proxyDto = proxyIdProxyMap.get(proxyId);
            if (proxyDto != null) {
                serverTree.put("proxy", proxyDto);
                String url = (String) serverTree.get("url");

                url += "&proxyType=" + proxyDto.getType().getCode() +
                        "&proxyIp=" + proxyDto.getIp() +
                        "&proxyPort=" + proxyDto.getPort() +
                        "&proxyRdns=true" +
                        "&proxyUser=" + proxyDto.getUsername() +
                        "&proxyPass=" + proxyDto.getPassword();
                serverTree.put("url", url);
            }
        }

    }


}


