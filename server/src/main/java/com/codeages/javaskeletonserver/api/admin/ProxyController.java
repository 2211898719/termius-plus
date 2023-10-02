package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.server.dto.ProxyCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyDto;
import com.codeages.javaskeletonserver.biz.server.dto.ProxySearchParams;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyUpdateParams;
import com.codeages.javaskeletonserver.biz.server.service.ProxyService;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
import java.util.List;

@RestController
@RequestMapping("/api-admin/proxy")
public class ProxyController {

    private final ProxyService proxyService;

    public ProxyController(ProxyService proxyService) {
        this.proxyService = proxyService;
    }

    @GetMapping("/search")
    public PagerResponse<ProxyDto> search(ProxySearchParams searchParams,
                                          @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                proxyService.search(searchParams, pager),
                pager
        );
    }

    @GetMapping("/list")
    public List<ProxyDto> list(ProxySearchParams searchParams) {
        return proxyService.search(searchParams, Pageable.unpaged()).getContent();
    }


    @PostMapping("/create")
    @RolesAllowed("ROLE_ADMIN")
    public ProxyDto create(@RequestBody ProxyCreateParams createParams) {
        return proxyService.create(createParams);
    }

    @PostMapping("/update")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse update(@RequestBody ProxyUpdateParams updateParams) {
        proxyService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        proxyService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


