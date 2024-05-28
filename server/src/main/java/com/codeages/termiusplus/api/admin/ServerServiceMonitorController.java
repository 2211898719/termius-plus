package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorDto;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorSearchParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorUpdateParams;
import com.codeages.termiusplus.biz.server.service.ServerServiceMonitorService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-admin/serverServiceMonitor")
public class ServerServiceMonitorController {

    private final ServerServiceMonitorService serverServiceMonitorService;

    public ServerServiceMonitorController(ServerServiceMonitorService serverServiceMonitorService) {
        this.serverServiceMonitorService = serverServiceMonitorService;
    }

    @GetMapping("/search")
    public PagerResponse<ServerServiceMonitorDto> search(@RequestParam ServerServiceMonitorSearchParams searchParams,
                                                         @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                serverServiceMonitorService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")

    public OkResponse create(@RequestBody ServerServiceMonitorCreateParams createParams) {
        serverServiceMonitorService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")

    public OkResponse update(@RequestBody ServerServiceMonitorUpdateParams updateParams) {
        serverServiceMonitorService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")

    public OkResponse delete(@RequestBody IdPayload idPayload) {
        serverServiceMonitorService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


