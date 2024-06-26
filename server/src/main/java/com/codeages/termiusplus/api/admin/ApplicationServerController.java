package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.application.dto.ApplicationServerCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerSearchParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerUpdateParams;
import com.codeages.termiusplus.biz.application.service.ApplicationServerService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;


@RestController
@RequestMapping("/api-admin/applicationServer")
public class ApplicationServerController {

    private final ApplicationServerService applicationServerService;

    public ApplicationServerController(ApplicationServerService applicationServerService) {
        this.applicationServerService = applicationServerService;
    }

    @GetMapping("/search")
    public PagerResponse<ApplicationServerDto> search(ApplicationServerSearchParams searchParams,
                                                      @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                applicationServerService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody ApplicationServerCreateParams createParams) {
        applicationServerService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ApplicationServerUpdateParams updateParams) {
        applicationServerService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        applicationServerService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


