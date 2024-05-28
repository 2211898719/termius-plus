package com.codeages.termiusplus.biz.scheduler.service;

public interface SchedulerService {

    void scheduleJob(IntervalJobDeclare declare);

    void scheduleJob(CronJobDeclare declare);

    void deleteJob(String name, String group);

    /**
     * 重置系统任务
     */
    void resetSystemJobs();
}
