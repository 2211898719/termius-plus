package com.codeages.termiusplus.api.admin;

import cn.hutool.core.collection.CollectionUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.extra.spring.SpringUtil;
import com.codeages.termiusplus.biz.application.dto.*;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.biz.application.service.ApplicationServerService;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.dto.ProxySearchParams;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.ws.ssh.EventType;
import com.codeages.termiusplus.ws.ssh.MessageDto;
import com.maxmind.geoip2.DatabaseReader;
import com.maxmind.geoip2.model.CityResponse;
import com.maxmind.geoip2.record.Location;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.aspectj.util.FileUtil;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;
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
    @GetMapping("/requestMap/{id}")
    public SseEmitter requestMap(@PathVariable("id") Long id) {
        SseEmitter sseEmitter = new SseEmitter();

        SSHClient sshClient = serverService.createSSHClient(id);
        net.schmizz.sshj.connection.channel.direct.Session shellSession = sshClient.startSession();
        shellSession.allocatePTY("xterm", 80, 24, 640, 480, Map.of());
        shellSession.setAutoExpand(true);

        Session.Shell shell = shellSession.startShell();

        InputStream inputStream = shell.getInputStream();
        OutputStream outputStream = shell.getOutputStream();

        try {
            outputStream.write("tail -100f  /var/log/nginx/open_access.log | awk '{print $1}'\n".getBytes());
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        DatabaseReader bean = SpringUtil.getBean(DatabaseReader.class);
        Thread thread = new Thread(() -> {
            byte[] buffer = new byte[1024];
            int i;
            //如果没有数据来，线程会一直阻塞在这个地方等待数据。
            while (true) {
                try {
                    if ((i = inputStream.read(buffer)) == -1) break;
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
                String originData = new String(Arrays.copyOfRange(buffer, 0, i), StandardCharsets.UTF_8);
                MessageDto messageDto = new MessageDto(EventType.COMMAND, originData);
                String[] ips = messageDto.getData().split("\n");
                for (String ip : ips) {
                    try {
                        ip=ip.trim();
                        InetAddress inetAddress = InetAddress.getByName(ip);
                        CityResponse response = bean.city(inetAddress);
                        if (response.getCity().getName() == null) {
                            System.out.println("IP地址：" + ip + " 未找到地理位置信息");
                            continue;
                        }
                        Location location = response.getLocation();
                        System.out.println("IP地址：" + ip + " 经纬度：" + location.getLongitude() + "," + location.getLatitude());
                        sseEmitter.send(Map.of("ip", ip, "longitude", location.getLongitude(), "latitude", location.getLatitude()));
                    } catch (Exception e) {
                        log.error("解析IP地址失败", e);
                    }
                }

            }
        });

        thread.start();

        return sseEmitter;
    }

    @GetMapping("/getApplicationRequestMap")
    public List<Map<String, Object>> getApplicationRequestMap(IdPayload idPayload) {
        DatabaseReader bean = SpringUtil.getBean(DatabaseReader.class);

        List<String> list = FileUtil.readAsLines(cn.hutool.core.io.FileUtil.file(
                "/Users/hongjunlong/Downloads/ip_list.txt"));
        list = list.stream().distinct().collect(Collectors.toList());
        int count = 0;
        List<Map<String, Object>> result = new ArrayList<>();
        for (String ip : list) {
            try {
                InetAddress inetAddress = InetAddress.getByName(ip);
                CityResponse response = bean.city(inetAddress);
                if (response.getCity().getName() == null) {
                    System.out.println("IP地址：" + ip + " 未找到地理位置信息");
                    count++;
                    continue;
                }
                Location location = response.getLocation();
                System.out.println("IP地址：" + ip + " 经纬度：" + location.getLongitude() + "," + location.getLatitude());
                result.add(Map.of("ip", ip, "longitude", location.getLongitude(), "latitude", location.getLatitude()));
            } catch (Exception e) {
                System.out.println("IP地址：" + ip + " 经纬度：" + "未知");
                count++;
            }
        }
        System.out.println("未找到地理位置信息的IP地址数量：" + count + "/" + list.size());

        return result;
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


