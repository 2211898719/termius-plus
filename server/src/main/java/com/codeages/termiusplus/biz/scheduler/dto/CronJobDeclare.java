package com.codeages.termiusplus.biz.scheduler.dto;

import org.quartz.Job;
import org.quartz.JobDataMap;

public class CronJobDeclare {

    private String name;

    private String group;

    private String cron;

    /**
     * 任务执行类
     */
    private Class<? extends Job> clazz;

    /**
     * 任务数据
     */
    private JobDataMap data = new JobDataMap();

    public static CronJobDeclare newJobDeclare() {
        return new CronJobDeclare();
    }

    public CronJobDeclare name(String name) {
        this.name = name;
        return this;
    }

    public CronJobDeclare group(String group) {
        this.group = group;
        return this;
    }

    public CronJobDeclare cron(String cron) {
        this.cron = cron;
        return this;
    }

    public CronJobDeclare clazz(Class<? extends Job> clazz) {
        this.clazz = clazz;
        return this;
    }

    public CronJobDeclare putData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public String getCron() {
        return cron;
    }

    public Class<? extends Job> getClazz() {
        return clazz;
    }

    public JobDataMap getData() {
        return data;
    }
}
