package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.application.dto.*;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api-admin/applicationMonitor")
public class ApplicationMonitorController {

    private final ApplicationMonitorService applicationMonitorService;

    public ApplicationMonitorController(ApplicationMonitorService applicationMonitorService) {
        this.applicationMonitorService = applicationMonitorService;
    }

    @GetMapping("/search")
    public PagerResponse<ApplicationMonitorDto> search(ApplicationMonitorSearchParams searchParams,
                                                       @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                applicationMonitorService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody ApplicationMonitorCreateParams createParams) {
        applicationMonitorService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ApplicationMonitorUpdateParams updateParams) {
        applicationMonitorService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        applicationMonitorService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }


    @PostMapping("/test")
    public ApplicationMonitorExecDto test(@RequestBody ApplicationMonitorDto createParams) {
        return applicationMonitorService.exec(createParams);
    }

}


