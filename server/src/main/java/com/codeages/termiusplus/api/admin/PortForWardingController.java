package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.PortForwarderDto;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.PortForWardingService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.codeages.termiusplus.common.IdPayload;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/port-forwarding")
public class PortForWardingController {
    private final PortForWardingService portForWardingService;
    private final ServerService serverService;

    @Autowired
    public PortForWardingController(PortForWardingService portForWardingService, ServerService serverService) {
        this.portForWardingService = portForWardingService;
        this.serverService = serverService;
    }

    @GetMapping("/list")
    public List<PortForwarderDto> list() {
        List<PortForwarderDto> list = portForWardingService.list();
        QueryUtils.batchQueryOneToOne(
                list,
                PortForwarderDto::getServerId,
                serverService::findByIdIn,
                ServerDto::getId,
                PortForwarderDto::setServerDto
                                     );
        return list;
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

    @GetMapping("/getLocalIp")
    public String getLocalIp() {
        return portForWardingService.getLocalIp();
    }
}
