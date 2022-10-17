package com.codeages.javaskeletonserver.biz.scheduler.service;

import org.quartz.Job;
import org.quartz.JobDataMap;

public class IntervalJobDeclare {

    private String name;

    private String group;

    private int every;

    private boolean zeroSecond = false;

    /**
     * 任务执行类
     */
    private Class<? extends Job> clazz;

    /**
     * 任务数据
     */
    private JobDataMap data = new JobDataMap();

    public static IntervalJobDeclare newJobDeclare() {
        return new IntervalJobDeclare();
    }

    public IntervalJobDeclare name(String name) {
        this.name = name;
        return this;
    }

    public IntervalJobDeclare group(String group) {
        this.group = group;
        return this;
    }

    public IntervalJobDeclare every(int every) {
        this.every = every;
        return this;
    }

    public IntervalJobDeclare zeroSecond() {
        this.zeroSecond = true;
        return this;
    }

    public IntervalJobDeclare clazz(Class<? extends Job> clazz) {
        this.clazz = clazz;
        return this;
    }

    public IntervalJobDeclare putData(String key, Object value) {
        data.put(key, value);
        return this;
    }

    public String getName() {
        return name;
    }

    public String getGroup() {
        return group;
    }

    public int getEvery() {
        return every;
    }

    public boolean isZeroSecond() {
        return zeroSecond;
    }

    public Class<? extends Job> getClazz() {
        return clazz;
    }

    public JobDataMap getData() {
        return data;
    }
}
