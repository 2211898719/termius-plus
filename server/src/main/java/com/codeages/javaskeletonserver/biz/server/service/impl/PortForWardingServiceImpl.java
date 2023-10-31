package com.codeages.javaskeletonserver.biz.server.service.impl;

import com.codeages.javaskeletonserver.biz.server.service.PortForWardingService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PortForWardingServiceImpl implements PortForWardingService {

    private final ServerService serverService;

    public PortForWardingServiceImpl(ServerService serverService) {
        this.serverService = serverService;
    }

    /**
     * 将本地端口转发到远程端口，使用本地host
     *
     * @param localPort
     * @param serverId
     * @param remotePort
     */
    @Override
    public void startPortForwarding(Integer localPort, Long serverId, Integer remotePort) {
//        Session session = serverService.createSession(serverId);
//        try {
//            session.setPortForwardingL(localPort, "", remotePort);
//        } catch (Exception e) {
//            log.error("端口转发失败", e);
//        }
    }
}
