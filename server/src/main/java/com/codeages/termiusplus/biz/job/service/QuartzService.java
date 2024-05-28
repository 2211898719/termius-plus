package com.codeages.termiusplus.biz.job.service;

import com.codeages.termiusplus.biz.job.dto.MvelCronCreateDto;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;

import java.util.List;

public interface QuartzService {

    void createCronJob(MvelCronCreateDto dto);

    void deleteJob(String jobName, String jobGroup);

    List<JobDetail> getCurrentlyExecutingJobs();

    void execJob(JobDataMap jobDataMap);
}
