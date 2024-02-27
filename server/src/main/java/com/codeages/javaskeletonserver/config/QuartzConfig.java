package com.codeages.javaskeletonserver.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import javax.sql.DataSource;

@Configuration
public class QuartzConfig {

    @Bean
    public SchedulerFactoryBean schedulerFactoryBean(DataSource dataSource) {
        SchedulerFactoryBean schedulerFactoryBean = new SchedulerFactoryBean();

        schedulerFactoryBean.setAutoStartup(true);
        schedulerFactoryBean.setDataSource(dataSource);
        schedulerFactoryBean.setTaskExecutor(taskExecutor()); // 关联线程池

        return schedulerFactoryBean;
    }

    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(1); // 设置核心线程池大小
        executor.setMaxPoolSize(1); // 设置最大线程池大小
        executor.setQueueCapacity(1000); // 设置队列容量
        executor.setThreadNamePrefix("QuartzThread-"); // 设置线程名称前缀
        executor.initialize();
        return executor;
    }
}
