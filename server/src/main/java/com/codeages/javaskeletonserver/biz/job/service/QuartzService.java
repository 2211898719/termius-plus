package com.codeages.javaskeletonserver.biz.job.service;

import com.codeages.javaskeletonserver.biz.job.dto.MvelCronCreateDto;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import java.util.List;

public interface QuartzService {

    void createCronJob(MvelCronCreateDto dto);

    void deleteJob(String jobName, String jobGroup);

    List<JobDetail> getCurrentlyExecutingJobs();

    void execJob(JobDataMap jobDataMap);
}
