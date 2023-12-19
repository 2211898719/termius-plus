package com.codeages.javaskeletonserver.biz.server.service;

import com.codeages.javaskeletonserver.biz.server.dto.PortForwarderDto;

import java.util.List;

public interface PortForWardingService {

    List<PortForwarderDto> list();

    void startPortForwarding(String forwardingName,
                             Integer localPort,
                             Long serverId,
                             Integer remotePort);

    void stopPortForwarding(Integer localPort);

    boolean isRunning(Integer localPort);

    void stopAllPortForwarding();
}
