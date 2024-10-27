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
import com.codeages.termiusplus.biz.job.dto.ExecuteCommandSSHClient;
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


    @SneakyThrows
    @GetMapping("/getServerLocation/{id}")
    public Map<String, Object> getServerLocation(@PathVariable("id") Long id) {
        List<ApplicationServerDto> servers = applicationServerService.getServers(id);

        ApplicationServerDto webServers = servers.stream().filter(e -> StrUtil.isNotBlank(e.getNginxLogPath())).findAny()
                                                       .orElseThrow(() -> new AppException(ErrorCode.INTERNAL_ERROR,"没有找到web服务器"));

        SSHClient sshClient = serverService.createSSHClient(webServers.getServerId());
        ExecuteCommandSSHClient executeCommandSSHClient = new ExecuteCommandSSHClient(sshClient);
        String localIPAddress = executeCommandSSHClient.getLocalIPAddress();
        DatabaseReader bean = SpringUtil.getBean(DatabaseReader.class);
        InetAddress inetAddress = InetAddress.getByName(localIPAddress);
        CityResponse response = bean.city(inetAddress);
        if (response.getCity().getName() == null) {
            return Map.of("ip", localIPAddress, "longitude", "未知", "latitude", "未知");
        }

        Location location = response.getLocation();
        return Map.of("ip", localIPAddress, "longitude", location.getLongitude(), "latitude", location.getLatitude());
    }

    @SneakyThrows
    @GetMapping("/requestMap/{id}")
    public SseEmitter requestMap(@PathVariable("id") Long id) {
        SseEmitter sseEmitter = new SseEmitter();
        List<ApplicationServerDto> servers = applicationServerService.getServers(id);

        List<ApplicationServerDto> webServers = servers.stream().filter(e -> StrUtil.isNotBlank(e.getNginxLogPath()))
                                                       .collect(Collectors.toList());

        for (ApplicationServerDto webServer : webServers) {
            SSHClient sshClient = serverService.createSSHClient(webServer.getServerId());
            net.schmizz.sshj.connection.channel.direct.Session shellSession = sshClient.startSession();
            shellSession.allocatePTY("xterm", 80, 24, 640, 480, Map.of());
            shellSession.setAutoExpand(true);

            Session.Shell shell = shellSession.startShell();

            InputStream inputStream = shell.getInputStream();
            OutputStream outputStream = shell.getOutputStream();

            outputStream.write(("tail -100f " + webServer.getNginxLogPath() + " | awk '{print $1}' \n").getBytes());
            outputStream.flush();

            DatabaseReader bean = SpringUtil.getBean(DatabaseReader.class);
            Thread thread = new Thread(() -> {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                loop:
                while (true) {
                    try {
                        if ((i = inputStream.read(buffer)) == -1) break;
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                    String originData = new String(Arrays.copyOfRange(buffer, 0, i), StandardCharsets.UTF_8);
                    String[] ips = originData.split("\n");
                    log.info("解析IP地址：{}", originData);
                    for (String ip : ips) {
                        try {
                            ip = ip.trim();
                            if (!ip.matches("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}")){
                                System.out.println("IP地址：" + ip + " 未找到地理位置信息");
                                continue;
                            }
                            InetAddress inetAddress = InetAddress.getByName(ip);
                            CityResponse response = bean.city(inetAddress);
                            if (response.getCity().getName() == null) {
                                System.out.println("IP地址：" + ip + " 未找到地理位置信息");
                                continue;
                            }
                            Location location = response.getLocation();
                            System.out.println("IP地址：" + ip + " 经纬度：" + location.getLongitude() + "," + location.getLatitude());
                            sseEmitter.send(Map.of(
                                    "ip",
                                    ip,
                                    "longitude",
                                    location.getLongitude(),
                                    "latitude",
                                    location.getLatitude()
                            ));
                        } catch (IllegalStateException e) {
                            log.error("请求结束关闭流", e);
                            break loop;
                        } catch (Exception e) {
                            log.error("解析IP地址失败", e);
                        }
                    }

                }
            });

            thread.start();
        }


        return sseEmitter;
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


