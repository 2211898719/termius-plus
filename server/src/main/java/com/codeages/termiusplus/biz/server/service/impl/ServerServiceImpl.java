package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.lang.tree.TreeNode;
import cn.hutool.core.lang.tree.TreeUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.entity.QServer;
import com.codeages.termiusplus.biz.server.entity.Server;
import com.codeages.termiusplus.biz.server.entity.ServerRunLog;
import com.codeages.termiusplus.biz.server.enums.OSEnum;
import com.codeages.termiusplus.biz.server.mapper.ServerMapper;
import com.codeages.termiusplus.biz.server.mapper.ServerRunLogMapper;
import com.codeages.termiusplus.biz.server.repository.ServerRepository;
import com.codeages.termiusplus.biz.server.repository.ServerRunLogRepository;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.codeages.termiusplus.biz.util.TreeUtils;
import com.codeages.termiusplus.biz.util.command.CpuUsage;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import com.codeages.termiusplus.biz.util.command.NetworkUsage;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.ws.ssh.AuthKeyBoardHandler;
import com.codeages.termiusplus.ws.ssh.EventType;
import com.codeages.termiusplus.ws.ssh.MessageDto;
import com.google.common.reflect.TypeToken;
import com.google.gson.Gson;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.transport.TransportException;
import net.schmizz.sshj.transport.verification.PromiscuousVerifier;
import net.schmizz.sshj.userauth.method.AuthPassword;
import net.schmizz.sshj.userauth.method.PasswordResponseProvider;
import net.schmizz.sshj.userauth.password.PasswordFinder;
import net.schmizz.sshj.userauth.password.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.net.SocketFactory;
import java.io.IOException;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ServerServiceImpl implements ServerService {

    private final ServerRunLogRepository serverRunLogRepository;

    private final ServerRunLogMapper serverRunLogMapper;

    private final ServerRepository serverRepository;

    private final ServerMapper serverMapper;

    private final Validator validator;

    private final ProxyService proxyService;

    private final Gson gson;


    @Value("${guacamole.serverId}")
    private Long guacamoleServerId;

    @Value("${server.connectCount:5}")
    private int connectCount;

    @Value("${serverInfo.timeout:60s}")
    private Duration timeout;

    @Value("${serverInfo.allTimeout:120s}")
    private Duration allTimeout;

    @Value("${guacamole.mapping}")
    private String guacamoleServerFilePath;


    private static ThreadPoolTaskExecutor executor;

    static {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(20); // 设置核心线程池大小
        executor.setMaxPoolSize(20); // 设置最大线程池大小
        executor.setQueueCapacity(5000); // 设置队列容量
        executor.setThreadNamePrefix("Monitor-"); // 设置线程名前缀
        executor.initialize();
    }

    public ServerServiceImpl(ServerRunLogRepository serverRunLogRepository, ServerRunLogMapper serverRunLogMapper,
                             ServerRepository serverRepository, ServerMapper serverMapper, Validator validator,
                             ProxyService proxyService,
                             Gson gson) {
        this.serverRunLogRepository = serverRunLogRepository;
        this.serverRunLogMapper = serverRunLogMapper;
        this.serverRepository = serverRepository;
        this.serverMapper = serverMapper;
        this.validator = validator;
        this.proxyService = proxyService;
        this.gson = gson;
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
            treeSortParams.get(i)
                          .setSort((long) i);
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
        return serverRepository.findAllById(ids)
                               .stream()
                               .map(serverMapper::toDto)
                               .collect(Collectors.toList());
    }

    @Override
    public List<ServerDto> findAllTestInfoServer() {
        return serverRepository.findAllByIsGroupAndOsAndInfoTest(false, OSEnum.LINUX, true)
                               .stream()
                               .map(serverMapper::toDto)
                               .collect(Collectors.toList());
    }

    private List<Server> toUpdateAllEntity(List<TreeSortParams> serverUpdateParams) {
        if (CollectionUtil.isEmpty(serverUpdateParams)) {
            return Collections.emptyList();
        }

        List<Server> servers = new ArrayList<>();

        for (TreeSortParams serverUpdateParam : serverUpdateParams) {
            servers.add(serverMapper.toUpdateAllEntity(
                    serverRepository.findById(serverUpdateParam.getId())
                                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND)),
                    serverUpdateParam
                                                      ));
            servers.addAll(toUpdateAllEntity(serverUpdateParam.getChildren()));
        }

        return servers;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        List<Server> children = serverRepository.findAllByParentId(id);
        if (CollectionUtil.isNotEmpty(children)) {
            children.stream()
                    .map(Server::getId)
                    .forEach(this::delete);
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
        server.setSort(maxSeqServer.map(Server::getSort)
                                   .orElse(0L) + 1);

        serverRepository.save(server);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Tree<Long>> findAll(List<Long> serverIds) {
        List<Server> allParent = findAllParent(serverRepository.findAllById(serverIds));
        allParent.addAll(findAllChildren(serverIds));

        List<ProxyDto> proxyDtoList = proxyService.search(new ProxySearchParams(), Pageable.unpaged())
                                                  .getContent();
        Map<Long, ProxyDto> proxyIdProxyMap = proxyDtoList.stream()
                                                          .collect(Collectors.toMap(
                                                                  ProxyDto::getId,
                                                                  Function.identity()
                                                                                   ));

        List<TreeNode<Long>> servers = allParent.stream()
                                                .map(e -> {
                                                    TreeNode<Long> longTreeNode = new TreeNode<>(
                                                            e.getId(),
                                                            e.getParentId(),
                                                            e.getName(),
                                                            e.getSort()
                                                    );
                                                    Map<String, Object> beanMap = BeanUtil.beanToMap(e);

                                                    longTreeNode.setExtra(beanMap);
                                                    if (e.getOs()
                                                         .equals(OSEnum.WINDOWS)) {
                                                        longTreeNode.getExtra()
                                                                    .put("guacamoleServerId", guacamoleServerId);
                                                        longTreeNode.getExtra()
                                                                    .put(
                                                                            "guacamoleServerFilePath",
                                                                            guacamoleServerFilePath
                                                                        );
                                                    }

                                                    return longTreeNode;
                                                })
                                                .collect(Collectors.toList());

        List<Tree<Long>> treeList = TreeUtil.build(servers, 0L);

        buildProxy(treeList, proxyIdProxyMap, null);

        return treeList;
    }

    @Override
    public boolean existServerByProxyId(Long proxyId) {
        return serverRepository.existsByProxyId(proxyId);
    }

    public List<Tree<Long>> findAllDb() {
        QServer q = QServer.server;
        BooleanBuilder builder = new BooleanBuilder();

        builder.and(q.isDb.eq(true));


        List<TreeNode<Long>> servers = findAllParent(serverRepository.findAll(builder, Pageable.unpaged())
                                                                     .getContent()).stream()
                                                                                   .map(e -> {
                                                                                       TreeNode<Long> longTreeNode = new TreeNode<>(
                                                                                               e.getId(),
                                                                                               e.getParentId(),
                                                                                               e.getName(),
                                                                                               e.getSort()
                                                                                       );

                                                                                       longTreeNode.setExtra(BeanUtil.beanToMap(
                                                                                               e));
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
            childrenIds = childrenServer.stream()
                                        .map(Server::getId)
                                        .collect(Collectors.toList());
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
                    serverTree.put("proxy", proxyIdProxyMap.get(proxyId));
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

    /**
     * 如果带有client sessionId 遇到可能需要二次验证的情况，会向前端发送获取验证码的消息
     *
     * @param id
     * @param sessionId
     * @return
     */
    @Override
    @SneakyThrows
    public SSHClient createSSHClient(Long id, String sessionId) {
        ServerDto server = findById(id);
        SSHClient ssh = new SSHClient();
        ssh.useCompression();
        ssh.setTimeout(3600 * 1000);
        ssh.setConnectTimeout(3600 * 1000);
        ssh.getTransport()
           .setTimeoutMs(0);
        if (Boolean.TRUE.equals(server.getKeepAlive())) {
            ssh.getConnection()
               .getKeepAlive()
               .setKeepAliveInterval(60);
        }
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
                public Socket createSocket(String host, int port, InetAddress localHost,
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
                public Socket createSocket(InetAddress address, int port, InetAddress localAddress,
                                           int localPort) throws IOException {
                    Socket socket = new Socket(createProxy(server));
                    socket.connect(new InetSocketAddress(address, port));
                    return socket;
                }
            });
        }

        // 设置主机和端口号
        ssh.addHostKeyVerifier(new PromiscuousVerifier());
        int errorCount = 0;
        AppException appException = new AppException(
                ErrorCode.INVALID_ARGUMENT,
                "连接失败，请检查网络或者服务器是否开启"
        );

        while (errorCount < connectCount) {
            try {
                ssh.connect(
                        server.getIp(),
                        server.getPort()
                              .intValue()
                           );
                break;
            } catch (ConnectException connectException) {
                appException = new AppException(ErrorCode.INVALID_ARGUMENT, "连接失败，请检查网络或者服务器是否开启");
                errorCount++;
            } catch (TransportException e) {
                appException = new AppException(ErrorCode.INVALID_ARGUMENT, "连接失败，请检查服务器配置");
                errorCount++;
            }
        }

        if (errorCount >= connectCount) {
            throw appException;
        }

        // 配置身份验证使用的私钥
        if (StrUtil.isNotBlank(server.getKey())) {
            ssh.authPublickey(server.getUsername(), ssh.loadKeys(server.getKey(), null, null));
        } else {
            PasswordFinder pfinder = new PasswordFinder() {
                @Override
                public char[] reqPassword(Resource<?> resource) {
                    return server.getPassword()
                                 .toCharArray()
                                 .clone();
                }

                @Override
                public boolean shouldRetry(Resource<?> resource) {
                    return true;
                }

            };

            log.info("ssh.isAuthenticated() = {}", ssh.isAuthenticated());
            ssh.auth(
                    server.getUsername(),
                    new AuthPassword(pfinder),
                    new CurrentAuthKeyboardInteractive(new PasswordResponseProvider(new PasswordFinder() {
                        @Override
                        public char[] reqPassword(Resource<?> resource) {
                            //如果没有sessionId 无从发起获取 keyboard interactive的验证码，所以无法进行登录
                            if (CharSequenceUtil.isBlank(sessionId)) {
                                return server.getPassword()
                                             .toCharArray()
                                             .clone();
                            }

                            String resourceKey = server.getName() + "-" + sessionId;
                            MessageDto messageDto = new MessageDto(EventType.AUTH_KEYBOARD, resourceKey);
                            log.info("向前端发送获取验证码的消息，message 是资源的验证码, messageDto = {}", messageDto);
                            //向前端发送获取验证码的消息，message 是资源的验证码
                            AuthKeyBoardHandler.sendMessage(sessionId, JSONUtil.toJsonStr(messageDto));
                            BlockingQueue<String> queue = ServerContext.AUTH_KEYBOARD_INTERACTIVE_POOL.getOrDefault(
                                    resourceKey,
                                    new LinkedBlockingQueue<>()
                                                                                                                   );
                            ServerContext.AUTH_KEYBOARD_INTERACTIVE_POOL.put(resourceKey, queue);
                            try {
                                String take = queue.take();
                                log.info("输入的验证码是：{}", take);
                                return take.toCharArray();
                            } catch (InterruptedException e) {
                                throw new RuntimeException(e);
                            }
                        }

                        @Override
                        public boolean shouldRetry(Resource<?> resource) {
                            return true;
                        }

                    }, Pattern.compile("P.* ", Pattern.DOTALL)))
                    );

        }

        return ssh;
    }

    @SneakyThrows
    public SSHClient createSSHClient(Long id) {
        return createSSHClient(id, null);
    }

    @Override
    @SneakyThrows
    public Proxy createProxy(ServerDto server) {
        ProxyDto proxyDto = server.getProxy();
        return new Proxy(
                proxyDto.getType()
                        .getType(),
                new InetSocketAddress(
                        proxyDto.getIp(),
                        proxyDto.getPort()
                                .intValue()
                )
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
        return serverRepository.findAllByParentId(0L)
                               .stream()
                               .map(Server::getId)
                               .collect(Collectors.toList());
    }

    /**
     * 获取某个服务器root用户的 history
     */

    @Override
    public List<String> getHistory(Long serverId) {
        return new ExecuteCommandSSHClient(serverId).getHistory("bash");
    }

    @Override
    public List<String> getMysqlHistory(Long serverId) {
        return new ExecuteCommandSSHClient(serverId).getHistory( "mysql").stream()
                                            .map(s -> s.replace("\\040", " "))
                                            .collect(Collectors.toList());
    }


    @Override
    public List<ServerRunLogDTO> syncAllServerRunInfo() {
        List<ServerDto> servers = findAllTestInfoServer();
        List<ServerRunLog> serverRunInfoDTOS = new CopyOnWriteArrayList<>();
        Date now = new Date();
        CompletableFuture<?>[] futures = servers.stream()
                                                .map(server ->
                                                             CompletableFuture.runAsync(() -> {
                                                                                  log.info(
                                                                                          "获取服务器运行信息，serverId = {}, serverName = {}, serverIp = {}",
                                                                                          server.getId(),
                                                                                          server.getName(),
                                                                                          server.getIp()
                                                                                          );
                                                                                  ExecuteCommandSSHClient sshClient = new ExecuteCommandSSHClient(
                                                                                          server.getId());

                                                                                  List<NetworkUsage> networkUsages = sshClient.calculateNetworkSpeed(
                                                                                          1);
                                                                                  CpuUsage cpuUsage = sshClient.getCpuUsage();
                                                                                  List<DiskUsage> diskUsage = sshClient.getDiskUsage();
                                                                                  ServerRunLog serverRunLog = new ServerRunLog();
                                                                                  serverRunLog.setServerId(server.getId());

                                                                                  serverRunLog.setCpuUsage(JSONUtil.toJsonStr(cpuUsage));
                                                                                  serverRunLog.setNetworkUsages(JSONUtil.toJsonStr(
                                                                                          networkUsages));
                                                                                  serverRunLog.setDiskUsages(JSONUtil.toJsonStr(diskUsage));
                                                                                  serverRunLog.setDate(now);
                                                                                  serverRunInfoDTOS.add(serverRunLog);
                                                                              }, executor)
                                                                              .orTimeout(
                                                                                      (int) allTimeout.toMillis(),
                                                                                      TimeUnit.MILLISECONDS
                                                                                        )
                                                                              .exceptionally(e -> {
                                                                                  log.error(
                                                                                          "获取服务器运行信息失败:{}:{}",
                                                                                          server.getName(),
                                                                                          server.getIp(),
                                                                                          e
                                                                                           );
                                                                                  return null;
                                                                              })
                                                    )
                                                .toArray(CompletableFuture[]::new);

        CompletableFuture.allOf(futures)
                         .join();

        serverRunLogRepository.saveAll(serverRunInfoDTOS);

        log.info("获取服务器运行信息成功，成功数量：{}/{}", serverRunInfoDTOS.size(), servers.size());

        return serverRunLogMapper.toDtoList(serverRunInfoDTOS);
    }

    @Override
    public List<ServerRunLogDTO> getServerLastRunInfoAfterLimit(Date date) {
        List<ServerRunLog> serverRunLogs = serverRunLogRepository.findByDateAfter(date);

        //有多个取最新一条
        return serverRunLogs.stream()
                            .collect(
                                    Collectors.toMap(
                                            ServerRunLog::getServerId,
                                            Function.identity(),
                                            (a, b) -> a.getDate()
                                                       .compareTo(b.getDate()) > 0 ? a : b
                                                    )
                                    )
                            .values()
                            .stream()
                            .map(serverRunLogMapper::toDto)
                            .collect(Collectors.toList());
    }

    @Override
    public List<ServerRunLogDTO> getServerLastRunInfoAfter(Date date) {
        return serverRunLogRepository.findByDateAfter(date)
                                     .stream()
                                     .map(serverRunLogMapper::toDto)
                                     .collect(Collectors.toList());
    }

    @Override
    public List<ServerRunLogDTO> getServerLastRunInfoAfter(Long serverId, Date date) {
        return serverRunLogRepository.findByServerIdAndDateAfter(serverId, date)
                                     .stream()
                                     .map(serverRunLogMapper::toDto)
                                     .collect(Collectors.toList());
    }


}


