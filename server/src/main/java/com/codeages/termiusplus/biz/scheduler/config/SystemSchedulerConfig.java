package com.codeages.termiusplus.biz.scheduler.config;

import com.codeages.termiusplus.biz.scheduler.dto.CronJobDeclare;
import com.codeages.termiusplus.biz.scheduler.dto.IntervalJobDeclare;

import java.util.ArrayList;
import java.util.List;

public interface SystemSchedulerConfig {

    default List<IntervalJobDeclare> declareIntervalJobs() {
        return new ArrayList<>();
    }

    default List<CronJobDeclare> declareCronJobs() {
        return new ArrayList<>();
    }
}
