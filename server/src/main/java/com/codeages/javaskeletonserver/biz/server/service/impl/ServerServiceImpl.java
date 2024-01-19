package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.servlet.ServletUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.dto.*;
import com.codeages.javaskeletonserver.biz.server.entity.QServer;
import com.codeages.javaskeletonserver.biz.server.entity.Server;
import com.codeages.javaskeletonserver.biz.server.mapper.ServerMapper;
import com.codeages.javaskeletonserver.biz.server.repository.ServerRepository;
import com.codeages.javaskeletonserver.biz.server.service.ProxyService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.util.TreeUtils;
import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import lombok.SneakyThrows;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.net.SocketFactory;
import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolation;
import javax.validation.Validator;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ServerServiceImpl implements ServerService {

    private final ServerRepository serverRepository;

    private final ServerMapper serverMapper;

    private final Validator validator;

    private final ProxyService proxyService;

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
        ServerDto currentServer = serverDto;
        while (proxyId == null) {
            if (currentServer.getParentId() == null || currentServer.getParentId() == 0) {
                break;
            }

            currentServer = serverRepository.findById(currentServer.getParentId())
                                            .map(serverMapper::toDto)
                                            .orElseThrow(() -> new AppException(
                                                    ErrorCode.INVALID_ARGUMENT,
                                                    "服务器不存在"
                                            ));
            proxyId = currentServer.getProxyId();
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

    @Override
    public List<ServerDto> findByIdIn(List<Long> ids) {
        return serverRepository.findAllById(ids).stream().map(serverMapper::toDto).collect(Collectors.toList());
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
    public List<Tree<Long>> findAll(List<Long> serverIds) {

        List<Server> allParent = findAllParent(serverRepository.findAllById(serverIds));
        allParent.addAll(findAllChildren(serverIds));

        List<ProxyDto> proxyDtoList = proxyService.search(new ProxySearchParams(), Pageable.unpaged()).getContent();
        Map<Long, ProxyDto> proxyIdProxyMap = proxyDtoList.stream()
                                                          .collect(Collectors.toMap(
                                                                  ProxyDto::getId,
                                                                  Function.identity()
                                                          ));

//        List<Server> sort = serverRepository.findAll(Sort.by("sort"));

        List<TreeNode<Long>> servers = allParent
                .stream()
                .map(e -> {
                    TreeNode<Long> longTreeNode = new TreeNode<>(
                            e.getId(),
                            e.getParentId(),
                            e.getName(),
                            e.getSort()
                    );

                    longTreeNode.setExtra(BeanUtil.beanToMap(e));
                    return longTreeNode;
                })
                .collect(Collectors.toList());

        List<Tree<Long>> treeList = TreeUtil.build(servers, 0L);

        buildProxy(treeList, proxyIdProxyMap, null);

        return treeList;
    }

    public List<Tree<Long>> findAllDb() {
        QServer q = QServer.server;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(q.isDb.eq(true));


        List<TreeNode<Long>> servers = findAllParent(serverRepository.findAll(builder, Pageable.unpaged()).getContent())
                .stream()
                .map(e -> {
                    TreeNode<Long> longTreeNode = new TreeNode<>(
                            e.getId(),
                            e.getParentId(),
                            e.getName(),
                            e.getSort()
                    );

                    longTreeNode.setExtra(BeanUtil.beanToMap(e));
                    return longTreeNode;
                })
                .collect(Collectors.toList());

        return TreeUtil.build(servers, 0L);
    }

    public List<Server> findAllParent(List<Server> servers) {
        List<Server> parentServers = new ArrayList<>(servers);
        List<Long> parentIds = servers.stream()
                                      .map(Server::getParentId)
                                      .filter(pId -> pId != 0)
                                      .collect(Collectors.toList());
        while (CollectionUtil.isNotEmpty(parentIds)) {
            List<Server> parentServer = serverRepository.findAllById(parentIds);
            parentServers.addAll(parentServer);
            parentIds = parentServer.stream()
                                    .map(Server::getParentId)
                                    .filter(pId -> pId != 0)
                                    .collect(Collectors.toList());
        }

        return parentServers;
    }

    public List<Server> findAllChildren(List<Long> servers) {
        List<Server> childrenServers = new ArrayList<>();
        List<Long> childrenIds = servers;
        while (CollUtil.isNotEmpty(childrenIds)) {
            List<Server> childrenServer = serverRepository.findAllByParentIdIn(childrenIds);
            childrenServers.addAll(childrenServer);
            childrenIds = childrenServer.stream().map(Server::getId).collect(Collectors.toList());
        }

        return childrenServers;
    }


    /**
     * 递归构建每个节点的代理，子节点的代理会覆盖父节点的代理，取就近原则
     */
    private void buildProxy(List<Tree<Long>> servers, Map<Long, ProxyDto> proxyIdProxyMap, Long parentProxyId) {
        if (CollectionUtil.isEmpty(servers)) {
            return;
        }

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
            }
        }

    }

    @SneakyThrows
    public SSHClient createSSHClient(Long id) {
        ServerDto server = findById(id);
        SSHClient ssh = new SSHClient();
        ssh.setTimeout(3600 * 1000);
        ssh.setConnectTimeout(3600 * 1000);
        ssh.getConnection().getKeepAlive().setKeepAliveInterval(60);
        //设置sshj代理
        if (server.getProxy() != null) {
            ssh.setSocketFactory(new SocketFactory() {

                public Socket createSocket() {
                    return new Socket(createProxy(server));
                }

                @Override
                public Socket createSocket(String host, int port) throws IOException {
                    Socket socket = new Socket(createProxy(server));
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(String host,
                                           int port,
                                           InetAddress localHost,
                                           int localPort) throws IOException {
                    Socket socket = new Socket(createProxy(server));
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(InetAddress host, int port) throws IOException {
                    Socket socket = new Socket(createProxy(server));
                    socket.connect(new InetSocketAddress(host, port));
                    return socket;
                }

                @Override
                public Socket createSocket(InetAddress address,
                                           int port,
                                           InetAddress localAddress,
                                           int localPort) throws IOException {
                    Socket socket = new Socket(createProxy(server));
                    socket.connect(new InetSocketAddress(address, port));
                    return socket;
                }
            });
        }

        // 设置主机和端口号
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        ssh.connect(server.getIp(), server.getPort().intValue());
        // 配置身份验证使用的私钥
        if (StrUtil.isNotBlank(server.getKey())) {
            ssh.authPublickey(server.getUsername(), ssh.loadKeys(server.getKey(), null, null));
        } else {
            ssh.authPassword(server.getUsername(), server.getPassword());
        }


        return ssh;
    }

    @Override
    @SneakyThrows
    public Proxy createProxy(ServerDto server) {
        ProxyDto proxyDto = server.getProxy();
        return new Proxy(
                proxyDto.getType().getType(),
                new InetSocketAddress(proxyDto.getIp(), proxyDto.getPort().intValue())
        );
    }

    @Override
    public List<Tree<Long>> groupList() {
        List<TreeNode<Long>> servers = serverRepository.findAllByIsGroupTrue()
                                                       .stream()
                                                       .map(e -> {
                                                           TreeNode<Long> longTreeNode = new TreeNode<>(
                                                                   e.getId(),
                                                                   e.getParentId(),
                                                                   e.getName(),
                                                                   e.getSort()
                                                           );
                                                           longTreeNode.setExtra(BeanUtil.beanToMap(e));
                                                           return longTreeNode;
                                                       })
                                                       .collect(Collectors.toList());

        Tree<Long> root = new Tree<>();
        root.setId(0L);
        root.setName("all");
        root.setChildren(TreeUtil.build(servers, 0L));
        return List.of(root);
    }

    @Override
    public List<Long> findAllTopId() {
        return serverRepository.findAllByParentId(0L).stream().map(Server::getId).collect(Collectors.toList());
    }

    /**
     * 获取网络延迟
     */
    public int getNetworkDelay(ServerDto serverDto) {
        return getNetworkDelay(serverDto.getIp());
    }

    /**
     * 获取当前请求的网络延迟
     */
    public int getCurrentRequestNetworkDelay(HttpServletRequest request) {
        return getNetworkDelay(ServletUtil.getClientIP(request));
    }

    /**
     * 获取使用代理后 到服务器的网络延迟
     */
    @SneakyThrows
    public int getNetworkDelay(Long id) {
        Socket socket = new Socket();
        ServerDto serverDto = findById(id);

        Proxy proxy = createProxy(serverDto);
        socket.connect(new InetSocketAddress(serverDto.getIp(), serverDto.getPort().intValue()), 5000);
        socket = new Socket(proxy);
        socket.connect(new InetSocketAddress(serverDto.getIp(), serverDto.getPort().intValue()), 5000);
        return (int) (System.currentTimeMillis() - socket.getSoTimeout());
    }

    private int getNetworkDelay(String ip) {
        try {
            long start = System.currentTimeMillis();
            InetAddress address = InetAddress.getByName(ip);

            boolean reachable = address.isReachable(5000);
            if (reachable) {
                return (int) (System.currentTimeMillis() - start);
            }
        } catch (Exception e) {
            return -1;
        }
        return -1;
    }


}


