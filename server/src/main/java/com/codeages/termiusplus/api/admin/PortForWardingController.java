package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;
import com.codeages.termiusplus.biz.server.service.PortForWardingService;
import com.codeages.termiusplus.common.IdPayload;
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
    public void startPortForwarding(@RequestBody IdPayload idPayload) {
        portForWardingService.start(idPayload.getId());
    }

    @PostMapping("/create")
    public void create(@RequestBody PortForwarderDto portForwarderDto) {
        portForWardingService.create(portForwarderDto);
    }

    @PostMapping("/update")
    public void updatePortForwarding(@RequestBody PortForwarderDto portForwarderDto) {
        portForWardingService.update(portForwarderDto);
    }

    @PostMapping("/stop")
    public void stopPortForwarding(@RequestBody IdPayload idPayload) {
        portForWardingService.stop(idPayload.getId());
    }

    @PostMapping("/delete")
    public void delete(@RequestBody IdPayload idPayload) {
        portForWardingService.delete(idPayload.getId());
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
