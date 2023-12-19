package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.server.dto.PortForwarderDto;
import com.codeages.javaskeletonserver.biz.server.service.PortForWardingService;
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
                portForwarderDto.getRemotePort()
        );
    }

    @PostMapping("/stop")
    public void stopPortForwarding(@RequestParam Integer localPort) {
        portForWardingService.stopPortForwarding(localPort);
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
