package com.codeages.termiusplus.biz.server.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.tree.Tree;
import com.codeages.termiusplus.biz.message.MessageService;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.dto.ServerRunLogDTO;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.apache.commons.math3.stat.regression.SimpleRegression;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Component
@Slf4j
public class ServerRunInfoJob {

    @Autowired
    private ServerService serverService;
    @Autowired
    private Gson gson;
    @Autowired
    private MessageService messageService;

    @Value("${server.estimateDiskUsageDays:90}")
    private int estimateDiskUsage;

    @Value("${server.estimateDiskUsageLogDays:365}")
    private int estimateDiskUsageLogDays;



    @Scheduled(cron = "0 0 0/4 *  * ?")
    @SchedulerLock(name = "ServerRunInfoJob_execute", lockAtMostFor = "10m")
    public void execute() {
        log.info("ServerRunInfoJob execute");
        serverService.syncAllServerRunInfo();
        log.info("ServerRunInfoJob execute end");
    }

    @PostConstruct
    @Scheduled(cron = "0 30 10 * * ?")
    @SchedulerLock(name = "ServerRunInfoJob_estimateDiskUsage", lockAtMostFor = "60s")
    //硬盘使用率估算
    public void estimateDiskUsage() {
        log.info("ServerRunInfoJob execute");
        long start = System.currentTimeMillis();

        List<ServerDto> allTestInfoServer = serverService.findAllTestInfoServer();
        Date now = new Date();

        BigDecimal estimateDiskUsageBigDecimal = new BigDecimal(estimateDiskUsage);
        Map<Long, Map<String, BigDecimal>> serverIdDiskUsageMap = new HashMap<>();
        for (ServerDto serverDto : allTestInfoServer) {

            Long serverId = serverDto.getId();
            List<ServerRunLogDTO> serverLastRunInfoAfter = serverService.getServerLastRunInfoAfter(
                    serverId,
                    DateUtil.offsetDay(
                            DateUtil.date(),
                            -estimateDiskUsageLogDays
                                      )
                                                                                                  );

            Type type = new TypeToken<List<DiskUsage>>() {}.getType();
            Map<String, List<Pair<Date, DiskUsage>>> serverDiskUsageMap = new HashMap<>();
            for (ServerRunLogDTO serverRunLogDTO : serverLastRunInfoAfter) {
                List<DiskUsage> diskUsageList = gson.fromJson(
                        serverRunLogDTO.getDiskUsages(),
                        type
                                                             );
                for (DiskUsage diskUsage : diskUsageList) {
                    List<Pair<Date, DiskUsage>> list = serverDiskUsageMap.getOrDefault(
                            diskUsage.getFilesystem(),
                            new ArrayList<>()
                                                                                      );

                    list.add(Pair.of(serverRunLogDTO.getDate(), diskUsage));
                    serverDiskUsageMap.put(diskUsage.getFilesystem(), list);
                }
            }

            Map<String, BigDecimal> allDiskUsageMap = new HashMap<>();
            serverDiskUsageMap.forEach((filesystem, diskUsageList) -> {
                if (isWithin2Days(diskUsageList.getLast()
                                               .getKey(), now)) {
                    try {
                        BigDecimal day = estimateDiskExhaustion(diskUsageList);
                        allDiskUsageMap.put(filesystem, day);
                    } catch (Exception e) {
                        log.error("estimateDiskExhaustion error:{},{}", serverId, filesystem);
                    }

                }
            });

            serverIdDiskUsageMap.put(serverId, allDiskUsageMap);
        }


        List<Tree<Long>> all = serverService.findAll(List.of(0L));

        String title = "硬盘使用率预警";
        String content = "\n ## 服务器硬盘即将耗尽，请尽快处理：\n";

        List<String> bodyList = new ArrayList<>();
        for (Map.Entry<Long, Map<String, BigDecimal>> entry : serverIdDiskUsageMap.entrySet()) {
            Long serverId = entry.getKey();
            Map<String, BigDecimal> value = entry.getValue();
            List<CharSequence> groupPath = findGroupPath(all, serverId);
            String current = " ### " + CollUtil.join(groupPath, " > ") + "\n";
            List<String> diskList = new ArrayList<>();
            value.forEach((filesystem, day) -> {
                //逆增长
                if (day == null) {
                    return;
                }

                //不到预警阈值
                if (estimateDiskUsageBigDecimal.compareTo(day) <= 0) {
                    return;
                }

                diskList.add(" - " + filesystem + "：" + day + "天");
            });

            if (CollUtil.isNotEmpty(diskList)) {
                current += String.join("\n", diskList);
                bodyList.add(current);
            }

        }


        if (CollUtil.isEmpty(bodyList)) {
            return;
        }

        messageService.send(MessageSubType.MARKDOWN, title, content + String.join("\n", bodyList));

        log.info("ServerRunInfoJob execute end 耗时:{}", System.currentTimeMillis() - start);
    }


    private List<CharSequence> findGroupPath(List<Tree<Long>> all, Long applicationId) {
        for (Tree<Long> tree : all) {
            if (tree.getId()
                    .equals(applicationId)) {
                return tree.getParentsName(true)
                           .reversed();
            } else if (CollUtil.isNotEmpty(tree.getChildren())) {
                List<CharSequence> applicationGroupPath = findGroupPath(tree.getChildren(), applicationId);
                if (CollUtil.isNotEmpty(applicationGroupPath)) {
                    return applicationGroupPath;
                }
            }
        }

        return Collections.emptyList();
    }

    //判断时间是否为2天之内
    private boolean isWithin2Days(Date date1, Date date2) {
        return DateUtil.between(date1, date2, DateUnit.DAY) <= 2;
    }

    public static BigDecimal estimateDiskExhaustion(List<Pair<Date, DiskUsage>> usages) {
        if (usages.size() < 2) {
            return null;
        }

        DiskUsage last = usages.getLast()
                               .getValue();
        BigDecimal currentAvailable = new BigDecimal(last.getAvailable());
        SimpleRegression regression = new SimpleRegression();
        for (Pair<Date, DiskUsage> pair : usages) {
            DiskUsage usage = pair.getValue();
            // 假设 usages 是按时间排序的
            //可能会超出double的范围，需要转换成BigDecimal
            regression.addData(
                    pair.getKey()
                        .getTime(),
                    new BigDecimal(usage.getUsed()).divide(BigDecimal.valueOf(1024))
                                                   .doubleValue()
                              );
        }

        double slope = regression.getSlope();
        //扩大1024
        BigDecimal slopeBigDecimal = new BigDecimal(slope);


        if (slope > 0) {
            BigDecimal decimal = currentAvailable.divide(BigDecimal.valueOf(1024))
                                                 .divide(slopeBigDecimal, 2, RoundingMode.HALF_UP);

            return decimal.divide(BigDecimal.valueOf(3600000 * 24), 0, RoundingMode.HALF_UP);
        }

        return null;
    }

}
