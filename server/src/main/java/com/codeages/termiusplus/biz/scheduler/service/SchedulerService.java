package com.codeages.termiusplus.biz.scheduler.service;

import com.codeages.termiusplus.biz.scheduler.dto.CronJobDeclare;
import com.codeages.termiusplus.biz.scheduler.dto.IntervalJobDeclare;

public interface SchedulerService {

    void scheduleJob(IntervalJobDeclare declare);

    void scheduleJob(CronJobDeclare declare);

    void deleteJob(String name, String group);

    /**
     * 重置系统任务
     */
    void resetSystemJobs();
}
