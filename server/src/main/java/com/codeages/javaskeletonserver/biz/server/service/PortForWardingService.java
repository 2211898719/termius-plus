package com.codeages.javaskeletonserver.biz.server.service;

public interface PortForWardingService {

    void startPortForwarding(Integer localPort, Long serverId, Integer remotePort);
}
