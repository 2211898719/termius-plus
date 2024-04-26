package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationUpdateParams;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationService;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/application")
public class ApplicationController {

    private final ApplicationService applicationService;

    public ApplicationController(ApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @GetMapping("/list")
    public List<Tree<Long>> findAll() {
        return applicationService.findAll();
    }

    @PostMapping("/updateSort")
    public OkResponse updateSort(@RequestBody List<TreeSortParams> treeSortParams) {
        applicationService.sort(treeSortParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody ApplicationCreateParams createParams) {
        applicationService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ApplicationUpdateParams updateParams) {
        applicationService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        applicationService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


