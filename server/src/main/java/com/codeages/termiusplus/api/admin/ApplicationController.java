package com.codeages.termiusplus.api.admin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.util.StrUtil;
import cn.hutool.extra.spring.SpringUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.application.dto.*;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.biz.application.service.ApplicationServerService;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.dto.ProxySearchParams;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.exception.AppException;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import lombok.AllArgsConstructor;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api-admin/application")
@AllArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    private final ApplicationMonitorService applicationMonitorService;

    private final ApplicationServerService applicationServerService;

    private final ProxyService proxyService;

    private final ServerService serverService;

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

        Map<Long, ProxyDto> idProxyDto = proxyService.search(new ProxySearchParams(), Pageable.unpaged()).getContent().stream()
                .collect(Collectors.toMap(ProxyDto::getId, Function.identity()));


        if (CollectionUtil.isNotEmpty(applicationServiceAll)) {
            applicationServiceAll.forEach(tree -> tree.walk(n -> {
                ApplicationMonitorDto applicationMonitorDto = applicationMonitorMap.get(n.getId());
                if (applicationMonitorDto != null) {
                    n.putExtra("monitorType", applicationMonitorDto.getType());
                    n.putExtra("monitorConfig", applicationMonitorDto.getConfig());
                    n.putExtra("remark", applicationMonitorDto.getRemark());
                    n.putExtra("failureCount", applicationMonitorDto.getFailureCount());
                }

                if (n.get("proxyId") != null) {
                    Long proxyId = Long.parseLong(n.get("proxyId").toString());
                    ProxyDto proxyDto = idProxyDto.get(proxyId);
                    if (proxyDto != null) {
                        n.putExtra("proxy", proxyDto);
                    }
                }

                List<ApplicationServerDto> applicationServerDtoList = applicationServerMap.get(n.getId());
                if (CollectionUtil.isNotEmpty(applicationServerDtoList)) {
                    n.putExtra("serverList", applicationServerDtoList);
                }
            }));

            setGroupFailureCount(applicationServiceAll);
        }

        return applicationServiceAll;
    }

    //递归计算 节点的failureCount是子节点的failureCount之和否则为null
    private void setGroupFailureCount(List<Tree<Long>> applicationService) {
        for (Tree<Long> tree : applicationService) {
            if (CollectionUtil.isNotEmpty(tree.getChildren())) {
                setGroupFailureCount(tree.getChildren());
                Long failureCount = tree.getChildren().stream()
                        .map(n -> n.get("failureCount"))
                        .filter(Objects::nonNull)
                        .map(Long.class::cast)
                        .reduce(0L, Long::sum);
                tree.putExtra("failureCount", failureCount);
            } else {
                tree.putExtra("failureCount", tree.get("failureCount"));
            }
        }
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

    @GetMapping("/groupList")
    public List<Tree<Long>> groupList() {
        return applicationService.groupList();
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

    @GetMapping("/getApplicationErrorRank")
    public List<ApplicationMonitorLogCountDto> getApplicationErrorRank() {
        List<ApplicationMonitorLogCountDto> applicationErrorRank = applicationMonitorService.getApplicationErrorRank();
        QueryUtils.batchQueryOneToOne(
                applicationErrorRank,
                ApplicationMonitorLogCountDto::getApplicationId,
                applicationService::findAllByIds,
                ApplicationDto::getId,
                (applicationMonitorLogCountDto, applicationDto) -> {
                    applicationMonitorLogCountDto.setApplicationName(applicationDto.getName());
                    applicationMonitorLogCountDto.setApplicationContent(applicationDto.getContent());
                }
        );

        return applicationErrorRank;
    }

}


