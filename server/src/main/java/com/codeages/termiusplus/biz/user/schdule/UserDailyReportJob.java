package com.codeages.termiusplus.biz.user.schdule;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

/**
 * 用户每日报告 Job
 */
@Slf4j
@Component
public class UserDailyReportJob extends QuartzJobBean {

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        log.info("UserDailyReportJob execute");
    }
}
