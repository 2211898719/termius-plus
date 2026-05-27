package com.codeages.termiusplus.biz.patrol.job;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PatrolScheduledJob {

    private final PatrolEngine patrolEngine;

    @Scheduled(cron = "${patrol.schedule.cron:0 0 2,14 * * ?}")
    @SchedulerLock(name = "PatrolScheduledJob_execute", lockAtMostFor = "10m")
    public void execute() {
        log.info("定时巡检任务开始执行");
        long start = System.currentTimeMillis();

        try {
            List<PatrolTaskDto> results = patrolEngine.executeAll();
            long errorCount = results.stream().filter(r -> "error".equals(r.getStatus())).count();
            long warningCount = results.stream().filter(r -> "warning".equals(r.getStatus())).count();
            log.info("定时巡检完成: 总数={}, 错误={}, 警告={}, 耗时={}ms",
                    results.size(), errorCount, warningCount, System.currentTimeMillis() - start);
        } catch (Exception e) {
            log.error("定时巡检任务执行失败", e);
        }
    }
}
