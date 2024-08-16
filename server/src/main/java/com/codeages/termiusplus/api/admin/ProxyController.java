package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

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
    public ProxyDto create(@RequestBody ProxyCreateParams createParams) {
        return proxyService.create(createParams);
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ProxyUpdateParams updateParams) {
        proxyService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        proxyService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }


    @GetMapping("/getClashProxy")
    public ClashProxyDTO getClashProxy() {
        return proxyService.getClashProxy();
    }

}


