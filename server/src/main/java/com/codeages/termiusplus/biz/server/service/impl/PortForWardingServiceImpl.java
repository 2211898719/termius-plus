package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.entity.PortForwarding;
import com.codeages.termiusplus.biz.server.enums.PortForWardingStatusEnum;
import com.codeages.termiusplus.biz.server.mapper.PortForwardingMapper;
import com.codeages.termiusplus.biz.server.repository.PortForWardingRepository;
import com.codeages.termiusplus.biz.server.service.PortForWardingService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.exception.AppException;
import jakarta.annotation.PostConstruct;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PortForWardingServiceImpl implements PortForWardingService {

    private final PortForWardingRepository portForWardingRepository;

    private final ServerService serverService;

    private final Map<Integer, PortForwarderDto> localPortForwarderMap;

    private final PortForwardingMapper portForwardingMapper;

    private final ThreadPoolTaskExecutor threadPoolTaskExecutor;

    @Value("${current.ip}")
    private String currentIp;

    @Value("${PortForWarding.maxPort}")
    private Integer maxPort;

    @PostConstruct
    public void init() {
        CompletableFuture.runAsync(
                () -> portForWardingRepository.findAllByStatus(PortForWardingStatusEnum.START)
                                              .forEach(this::startPortForwarding),
                threadPoolTaskExecutor
                                  );
    }

    public PortForWardingServiceImpl(PortForWardingRepository portForWardingRepository, ServerService serverService,
                                     PortForwardingMapper portForwardingMapper,
                                     @Qualifier("portForwardTaskExecutor") ThreadPoolTaskExecutor threadPoolTaskExecutor) {
        this.portForWardingRepository = portForWardingRepository;
        this.serverService = serverService;
        this.threadPoolTaskExecutor = threadPoolTaskExecutor;
        this.localPortForwarderMap = new ConcurrentHashMap<>();
        this.portForwardingMapper = portForwardingMapper;
    }

    @Override
    public List<PortForwarderDto> list() {
        return portForWardingRepository.findAll()
                                       .stream()
                                       .map(portForwardingMapper::toDto)
                                       .peek(d -> d.setLocalHost(currentIp))
                                       .toList();
    }

    @SneakyThrows
    private PortForwarderDto startPortForwarding(PortForwarding portForwarding) {
        return startPortForwarding(
                portForwarding.getId(),
                portForwarding.getForwardingName(),
                currentIp,
                portForwarding.getLocalPort(),
                portForwarding.getServerId(),
                portForwarding.getRemoteHost(),
                portForwarding.getRemotePort(),
                0
                                  );
    }

    @SneakyThrows
    private PortForwarderDto startPortForwarding(Long id,
                                                 String forwardingName,
                                                 Integer localPort,
                                                 Long serverId,
                                                 String remoteHost,
                                                 Integer remotePort) {
        return startPortForwarding(id, forwardingName, currentIp, localPort, serverId, remoteHost, remotePort, 0);
    }


    /**
     * 将本地端口转发到远程端口，使用本地host
     *
     * @param localPort
     * @param serverId
     * @param remotePort
     */
    private PortForwarderDto startPortForwarding(
            Long id,
            String forwardingName,
            String localHost,
            Integer localPort,
            Long serverId,
            String remoteHost,
            Integer remotePort,
            Integer retryCount) {
        if (localPortForwarderMap.containsKey(localPort)) {
            throw new AppException(
                    ErrorCode.INTERNAL_ERROR,
                    "端口已被“" + localPortForwarderMap.get(localPort)
                                                       .getForwardingName() + "”映射占用"
            );
        }

        if (!NetUtil.isUsableLocalPort(localPort)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口已被其他应用占用");
        }

        ServerDto serverDto = serverService.findById(serverId);

        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(localPort);
        } catch (IOException e) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口已被其他应用占用");
        }
        LocalPortForwarder localPortForwarder = null;
        SSHClient sshClient = null;
        try {
            sshClient = serverService.createSSHClient(serverId, 30000);
            localPortForwarder = sshClient
                    .newLocalPortForwarder(
                            new Parameters(
                                    localHost,
                                    localPort,
                                    CharSequenceUtil.isEmpty(remoteHost) ? serverDto.getIp() : remoteHost,
                                    remotePort
                            ),
                            serverSocket
                                          );
        } catch (Exception e) {
            log.error("端口转发失败", e);
            try {
                serverSocket.close();
            } catch (IOException ignored) {
            }
            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口转发失败");
        }

        ServerSocket finalServerSocket = serverSocket;
        LocalPortForwarder finalLocalPortForwarder = localPortForwarder;
        SSHClient finalSshClient = sshClient;
        CompletableFuture.runAsync(
                () -> {
                    Thread thread = threadPoolTaskExecutor.createThread(() -> {});
                    try {
                        finalLocalPortForwarder.listen(thread);
                    } catch (IOException e) {
                        log.error("端口转发失败", e);
                        try {
                            finalLocalPortForwarder.close();
                        } catch (IOException ignored) {

                        }
                        try {
                            thread.interrupt();
                        } catch (Exception ignored) {

                        }
                        try {
                            finalServerSocket.close();
                        } catch (Exception ignored) {

                        }
                        try {
                            finalSshClient.close();
                        } catch (Exception ignored) {

                        }

                        PortForwarderDto portForwarderDto = localPortForwarderMap.get(localPort);
                        if (portForwarderDto != null) {
                            localPortForwarderMap.remove(localPort);
                        }

                        if (retryCount <= 0) {
                            portForWardingRepository.findById(id)
                                                    .ifPresent(p -> {
                                                        p.setStatus(PortForWardingStatusEnum.STOP);
                                                        portForWardingRepository.save(p);
                                                    });
                            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口转发失败");
                        }

                        startPortForwarding(
                                id,
                                forwardingName,
                                localHost,
                                localPort,
                                serverId,
                                remoteHost,
                                remotePort,
                                portForwarderDto == null ? 0 : portForwarderDto.getRetryCount() + 1
                                           );
                    }
                },
                threadPoolTaskExecutor
                                  );

        PortForwarderDto portForwarderDto = new PortForwarderDto(
                id,
                forwardingName,
                localPortForwarder,
                sshClient,
                localPort,
                localHost,
                remoteHost,
                remotePort,
                serverId,
                serverDto,
                retryCount,
                null
        );

        localPortForwarderMap.put(localPort, portForwarderDto);

        return portForwarderDto;
    }

    @SneakyThrows
    private void stopPortForwarding(Integer localPort) {
        PortForwarderDto portForwarderDto = localPortForwarderMap.get(localPort);
        if (portForwarderDto == null) {
            log.warn("端口转发不存在，localPort:{}", localPort);
            return;
        }

        LocalPortForwarder localPortForwarder = portForwarderDto.getLocalPortForwarder();
        SSHClient sshClient = portForwarderDto.getSshClient();

        try {
            localPortForwarder.close();
        } catch (Exception e) {
            log.error("关闭端口转发失败", e);
        } finally {
            localPortForwarderMap.remove(localPort);
        }

        try {
            sshClient.close();
        } catch (Exception ignored) {

        }
    }

    @Override
    public boolean isRunning(Integer localPort) {
        return localPortForwarderMap.containsKey(localPort);
    }

    @Override
    public void stopAllPortForwarding() {
        localPortForwarderMap.forEach((k, v) -> {
            try {
                v.getLocalPortForwarder()
                 .close();
            } catch (IOException e) {
                log.error("关闭端口转发失败", e);
            }
        });

        localPortForwarderMap.clear();
    }

    @Override
    public void delete(Long id) {
        PortForwarding portForwarding = portForWardingRepository.findById(id)
                                                                .orElseThrow(() -> new AppException(
                                                                        ErrorCode.NOT_FOUND,
                                                                        "端口转发不存在"
                                                                ));
        if (Objects.equals(PortForWardingStatusEnum.START, portForwarding.getStatus())) {
            stopPortForwarding(portForwarding.getLocalPort());
        }

        portForWardingRepository.deleteById(id);
    }

    @Override
    public void create(PortForwarderDto portForwarderDto) {
        PortForwarding entity = portForwardingMapper.toEntity(portForwarderDto);
        entity.setStatus(PortForWardingStatusEnum.STOP);

        portForWardingRepository.save(entity);
    }

    @Override
    public void start(Long id) {
        PortForwarding portForwarding = portForWardingRepository.findById(id)
                                                                .orElseThrow(() -> new AppException(
                                                                        ErrorCode.NOT_FOUND,
                                                                        "端口转发不存在"
                                                                ));

        if (portForwarding.getStatus()
                          .equals(PortForWardingStatusEnum.START)) {
            stopPortForwarding(portForwarding.getLocalPort());
        }

        startPortForwarding(
                id,
                portForwarding.getForwardingName(),
                portForwarding.getLocalPort(),
                portForwarding.getServerId(),
                portForwarding.getRemoteHost(),
                portForwarding.getRemotePort()
                           );

        portForwarding.setStatus(PortForWardingStatusEnum.START);
        portForWardingRepository.save(portForwarding);
    }

    @Override
    public void update(PortForwarderDto portForwarderDto) {
        PortForwarding portForwarding = portForWardingRepository.findById(portForwarderDto.getId())
                                                                .orElseThrow(() -> new AppException(
                                                                        ErrorCode.NOT_FOUND,
                                                                        "端口转发不存在"
                                                                ));
        portForwarding.setForwardingName(portForwarderDto.getForwardingName());
        portForwarding.setServerId(portForwarderDto.getServerId());
        portForwarding.setLocalPort(portForwarderDto.getLocalPort());
        portForwarding.setRemoteHost(portForwarderDto.getRemoteHost());
        portForwarding.setRemotePort(portForwarderDto.getRemotePort());

        if (portForwarding.getStatus()
                          .equals(PortForWardingStatusEnum.START)) {
            stopPortForwarding(portForwarding.getLocalPort());
            portForwarding.setStatus(PortForWardingStatusEnum.STOP);
        }

        portForWardingRepository.save(portForwarding);
    }

    @Override
    public void stop(Long id) {
        PortForwarding portForwarding = portForWardingRepository.findById(id)
                                                                .orElseThrow(() -> new AppException(
                                                                        ErrorCode.NOT_FOUND,
                                                                        "端口转发不存在"
                                                                ));
        stopPortForwarding(portForwarding.getLocalPort());

        portForwarding.setStatus(PortForWardingStatusEnum.STOP);
        portForWardingRepository.save(portForwarding);
    }

    @Override
    public String getLocalIp() {
        return currentIp;
    }

}
