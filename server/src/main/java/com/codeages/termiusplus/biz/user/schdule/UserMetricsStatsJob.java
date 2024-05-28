package com.codeages.termiusplus.biz.user.schdule;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * 用户指标 Job
 */
@Component
@Slf4j
public class UserMetricsStatsJob extends QuartzJobBean {
    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("UserMetricsStatsJob execute");
    }
}
