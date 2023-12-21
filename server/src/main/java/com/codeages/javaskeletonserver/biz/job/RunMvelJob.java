package com.codeages.javaskeletonserver.biz.job;

import cn.hutool.extra.spring.SpringUtil;
import com.codeages.javaskeletonserver.biz.job.service.QuartzService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;

@Slf4j
public class RunMvelJob implements Job {

    @Override
    @SneakyThrows
    public void execute(JobExecutionContext context) {
        log.info(
                "开始执行任务，任务名称：{}，任务分组：{}",
                context.getJobDetail().getKey().getName(),
                context.getJobDetail().getKey().getGroup()
        );

        JobDataMap jobDataMap = context.getJobDetail().getJobDataMap();

        SpringUtil.getBean(QuartzService.class).execJob(jobDataMap);
    }

}
