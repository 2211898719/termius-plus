package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.net.NetUtil;
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
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PortForWardingServiceImpl implements PortForWardingService {

    private final PortForWardingRepository portForWardingRepository;

    private final ServerService serverService;

    private final Map<Integer, PortForwarderDto> localPortForwarderMap;
    private final PortForwardingMapper portForwardingMapper;

    @Value("${current.ip}")
    private String currentIp;

    @Value("${PortForWarding.maxPort}")
    private Integer maxPort;

    public PortForWardingServiceImpl(PortForWardingRepository portForWardingRepository, ServerService serverService,
                                     PortForwardingMapper portForwardingMapper) {
        this.portForWardingRepository = portForWardingRepository;
        this.serverService = serverService;
        this.localPortForwarderMap = new ConcurrentHashMap<>();
        this.portForwardingMapper = portForwardingMapper;
    }

    @Override
    public List<PortForwarderDto> list() {
        return new ArrayList<>(localPortForwarderMap.values());
    }

    @SneakyThrows
    private PortForwarderDto startPortForwarding(Long id, String forwardingName,
                                                Integer localPort,
                                                Long serverId,
                                                String remoteHost,
                                                Integer remotePort) {
        return startPortForwarding(id,forwardingName, currentIp, localPort, serverId, remoteHost, remotePort, 0);
    }


    /**
     * 将本地端口转发到远程端口，使用本地host
     *
     * @param localPort
     * @param serverId
     * @param remotePort
     */
    @SneakyThrows
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

        LocalPortForwarder localPortForwarder = serverService.createSSHClient(serverId)
                                                             .newLocalPortForwarder(
                                                                     new Parameters(
                                                                             localHost,
                                                                             localPort,
                                                                             StrUtil.isEmpty(remoteHost) ? serverDto.getIp() : remoteHost,
                                                                             remotePort
                                                                     ),
                                                                     new ServerSocket(localPort)
                                                                                   );


        CompletableFuture.runAsync(() -> {
            try {
                localPortForwarder.listen(ThreadUtil.newThread(
                        () -> {
                        }, "localPortForwarder-" + localPort
                                                              ));
            } catch (IOException e) {
                log.error("端口转发失败", e);
                try {
                    localPortForwarder.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }

                PortForwarderDto portForwarderDto = localPortForwarderMap.get(localPort);
                if (portForwarderDto != null) {
                    localPortForwarderMap.remove(localPort);
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
        });


        PortForwarderDto portForwarderDto = new PortForwarderDto(
                id,
                forwardingName,
                localPortForwarder,
                localPort,
                localHost,
                remoteHost,
                remotePort,
                serverId,
                serverDto,
                retryCount
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

        try {
            localPortForwarder.close();
        } catch (Exception e) {
            log.error("关闭端口转发失败", e);
        } finally {
            localPortForwarderMap.remove(localPort);
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
        if (portForwarding.getStatus()
                          .equals(PortForWardingStatusEnum.START)) {
            stopPortForwarding(portForwarding.getLocalPort());
        }


        portForWardingRepository.deleteById(id);
    }

    @Override
    public void create(PortForwarderDto portForwarderDto) {
        portForWardingRepository.save(portForwardingMapper.toEntity(portForwarderDto));
    }

    @Override
    public void start(Long id) {

    }

    @Override
    public void update(PortForwarderDto portForwarderDto) {

    }

    @Override
    public void stop(Long id) {

    }
}
