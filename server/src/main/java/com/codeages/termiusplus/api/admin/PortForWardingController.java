package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;
import com.codeages.termiusplus.biz.server.service.PortForWardingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/port-forwarding")
public class PortForWardingController {
    private final PortForWardingService portForWardingService;

    @Autowired
    public PortForWardingController(PortForWardingService portForWardingService) {
        this.portForWardingService = portForWardingService;
    }

    @GetMapping("/list")
    List<PortForwarderDto> list() {
        return portForWardingService.list();
    }

    @PostMapping("/start")
    public void startPortForwarding(@RequestBody PortForwarderDto portForwarderDto) {
        portForWardingService.startPortForwarding(
                portForwarderDto.getForwardingName(),
                portForwarderDto.getLocalPort(),
                portForwarderDto.getServerId(),
                portForwarderDto.getRemoteHost(),
                portForwarderDto.getRemotePort()
        );
    }

    @PostMapping("/stop")
    public void stopPortForwarding(@RequestBody PortForwarderDto portForwarderDto) {
        portForWardingService.stopPortForwarding(portForwarderDto.getLocalPort());
    }

    @GetMapping("/isRunning")
    public boolean isRunning(@RequestParam Integer localPort) {
        return portForWardingService.isRunning(localPort);
    }

    @PostMapping("/stopAll")
    public void stopAllPortForwarding() {
        portForWardingService.stopAllPortForwarding();
    }
}
