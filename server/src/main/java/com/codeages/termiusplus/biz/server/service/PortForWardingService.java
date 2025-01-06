package com.codeages.termiusplus.biz.server.service;

import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;

import java.util.Collection;
import java.util.List;

public interface PortForWardingService {

    List<PortForwarderDto> list();

    PortForwarderDto startRetainPortForwarding(Long serverId,
                                               String remoteHost,
                                               Integer remotePort);

    PortForwarderDto startPortForwarding(String forwardingName,
                             Integer localPort,
                             Long serverId,
                             String remoteHost,
                             Integer remotePort);

    Integer startPortForwarding(String forwardingName,
                             Long serverId,
                             Integer remotePort);

    void stopPortForwarding(Integer localPort);

    boolean isRunning(Integer localPort);

    void stopAllPortForwarding();
}
