package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.application.dto.*;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationMonitorService;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationServerService;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationService;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api-admin/application")
public class ApplicationController {

    private final ApplicationService applicationService;

    private final ApplicationMonitorService applicationMonitorService;

    private final ApplicationServerService applicationServerService;

    public ApplicationController(ApplicationService applicationService,
                                 ApplicationMonitorService applicationMonitorService,
                                 ApplicationServerService applicationServerService) {
        this.applicationService = applicationService;
        this.applicationMonitorService = applicationMonitorService;
        this.applicationServerService = applicationServerService;
    }

    @GetMapping("/list")
    public List<Tree<Long>> findAll() {
        List<Tree<Long>> applicationServiceAll = applicationService.findAll();
        Map<Long, ApplicationMonitorDto> applicationMonitorMap = applicationMonitorService.search(
                new ApplicationMonitorSearchParams(),
                Pageable.unpaged()
        ).getContent().stream().collect(
                Collectors.toMap(ApplicationMonitorDto::getApplicationId, Function.identity()));

        Map<Long, List<ApplicationServerDto>> applicationServerMap = applicationServerService.search(
                                                                                        new ApplicationServerSearchParams(),
                                                                                        Pageable.unpaged()
                                                                                ).getContent().stream()
                                                                                .collect(Collectors.groupingBy(
                                                                                        ApplicationServerDto::getApplicationId));


        applicationServiceAll.forEach(tree -> tree.walk(n->{
            ApplicationMonitorDto applicationMonitorDto = applicationMonitorMap.get(n.getId());
            if (applicationMonitorDto!= null) {
                n.putExtra("monitorType", applicationMonitorDto.getType());
                n.putExtra("monitorConfig", applicationMonitorDto.getConfig());
                n.putExtra("remark", applicationMonitorDto.getRemark());
                n.putExtra("failureCount", applicationMonitorDto.getFailureCount());
            }


            List<ApplicationServerDto> applicationServerDtoList = applicationServerMap.get(n.getId());
            if (CollectionUtil.isNotEmpty(applicationServerDtoList )) {
                n.putExtra("serverList", applicationServerDtoList);
            }
        }));

        return applicationServiceAll;
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


