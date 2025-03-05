package com.codeages.termiusplus.biz.server.service;

import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;

import java.util.List;

public interface PortForWardingService {

    List<PortForwarderDto> list();

    boolean isRunning(Integer localPort);

    void stopAllPortForwarding();

    void delete(Long id);

    void create(PortForwarderDto portForwarderDto);

    void start(Long id);

    void update(PortForwarderDto portForwarderDto);

    void stop(Long id);

    String getLocalIp();
}
