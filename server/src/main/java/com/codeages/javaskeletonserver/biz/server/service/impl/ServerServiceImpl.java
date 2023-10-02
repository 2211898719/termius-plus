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
                                                                   "&password=" + encoder.encodeToString(e.getPassword()
                                                                                                          .getBytes());


                                                           if (e.getProxyId() != null) {
                                                               ProxyDto proxyDto = proxyIdProxyMap.get(e.getProxyId());
                                                               if (proxyDto != null) {
                                                                   url += "&proxyType=" + proxyDto.getType().getCode() +
                                                                           "&proxyIp=" + proxyDto.getIp() +
                                                                           "&proxyPort=" + proxyDto.getPort() +
                                                                           "&proxyRdns=true" +
                                                                           "&proxyUser=" + proxyDto.getUsername() +
                                                                           "&proxyPass=" + proxyDto.getPassword();
                                                               }
                                                           }

                                                           longTreeNode.getExtra().put("url", url);
                                                           return longTreeNode;
                                                       })
                                                       .collect(Collectors.toList());

        return TreeUtil.build(servers, 0L);
    }

}


