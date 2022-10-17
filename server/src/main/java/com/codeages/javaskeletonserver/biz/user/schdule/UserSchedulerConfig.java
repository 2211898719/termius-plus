package com.codeages.javaskeletonserver.biz.user.schdule;

import com.codeages.javaskeletonserver.biz.scheduler.config.SystemSchedulerConfig;
import com.codeages.javaskeletonserver.biz.scheduler.service.CronJobDeclare;
import com.codeages.javaskeletonserver.biz.scheduler.service.IntervalJobDeclare;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class UserSchedulerConfig implements SystemSchedulerConfig {

    @Override
    public List<IntervalJobDeclare> declareIntervalJobs() {
        var definition = IntervalJobDeclare.newJobDeclare()
                .name("UserMetricsStats")
                .group("User")
                .clazz(UserMetricsStatsJob.class)
                .every(60)
                .zeroSecond()
                ;
        return List.of(definition);
    }

    @Override
    public List<CronJobDeclare> declareCronJobs() {
        var definition = CronJobDeclare.newJobDeclare()
                .name("UserDailyReport")
                .group("User")
                .clazz(UserDailyReportJob.class)
                .cron("0 0 0 ? * * *"); // 每天零点
        return List.of(definition);
    }
}
