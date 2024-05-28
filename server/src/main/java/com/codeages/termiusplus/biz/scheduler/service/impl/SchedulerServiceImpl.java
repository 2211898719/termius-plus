package com.codeages.termiusplus.biz.scheduler.service.impl;

import cn.hutool.core.date.DatePattern;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.scheduler.config.SystemSchedulerConfig;
import com.codeages.termiusplus.biz.scheduler.service.CronJobDeclare;
import com.codeages.termiusplus.biz.scheduler.service.IntervalJobDeclare;
import com.codeages.termiusplus.biz.scheduler.service.SchedulerService;
import com.codeages.termiusplus.exception.AppException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.*;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class SchedulerServiceImpl implements SchedulerService {

    private final Scheduler scheduler;

    private final ApplicationContext context;

    public SchedulerServiceImpl(Scheduler scheduler, ApplicationContext context) {
        this.scheduler = scheduler;
        this.context = context;
    }

    @Override
    public void scheduleJob(IntervalJobDeclare declare) {
        var job = JobBuilder.newJob(declare.getClazz())
                .withIdentity(declare.getName(), declare.getGroup())
                .storeDurably();

        var schedule = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(declare.getEvery())
                .repeatForever();

        var trigger = TriggerBuilder.newTrigger()
                .withIdentity(declare.getName(), declare.getGroup())
                .usingJobData(declare.getData())
                .withSchedule(schedule);

        if (declare.isZeroSecond()) {
            var formatDate = DateUtil.format(new DateTime(), DatePattern.NORM_DATETIME_MINUTE_FORMAT);
            var startAt = DateUtil.parse(formatDate);
            trigger.startAt(DateUtil.offsetMinute(startAt, 1));
        } else {
            trigger.startNow();
        }

        try {
            scheduler.scheduleJob(job.build(), trigger.build());
        } catch (SchedulerException e) {
            log.error("schedule job ({}) failed: {}", declare.getName(), e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void scheduleJob(CronJobDeclare declare) {
        var job = JobBuilder.newJob(declare.getClazz())
                .withIdentity(declare.getName(), declare.getGroup())
                .storeDurably();

        var schedule = CronScheduleBuilder.cronSchedule(declare.getCron());

        var trigger = TriggerBuilder.newTrigger()
                .withIdentity(declare.getName(), declare.getGroup())
                .usingJobData(declare.getData())
                .withSchedule(schedule);

        try {
            scheduler.scheduleJob(job.build(), trigger.build());
        } catch (SchedulerException e) {
            log.error("schedule job ({}) failed: {}", declare.getName(), e.getMessage());
            throw new AppException(ErrorCode.INTERNAL_ERROR);
        }
    }

    @Override
    public void deleteJob(String name, String group) {
        try {
            scheduler.deleteJob(new JobKey(name, group));
        } catch (SchedulerException e) {
            log.error("delete job ({}) failed: {}", name, e.getMessage());
        }
    }

    @Override
    public void resetSystemJobs() {
        var configs = context.getBeansOfType(SystemSchedulerConfig.class);
        for (var config : configs.values()) {
            for (var job: config.declareIntervalJobs()) {
                log.info("reset system job ({})", job.getName());
                deleteJob(job.getName(), job.getGroup());
                scheduleJob(job);
            }

            for (var job: config.declareCronJobs()) {
                log.info("reset system job ({})", job.getName());
                deleteJob(job.getName(), job.getGroup());
                scheduleJob(job);
            }
        }
    }
}
