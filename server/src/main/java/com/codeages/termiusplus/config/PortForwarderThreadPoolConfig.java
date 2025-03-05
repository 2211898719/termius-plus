package com.codeages.termiusplus.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;


@Configuration
public class PortForwarderThreadPoolConfig {

    @Bean(name = "portForwardTaskExecutor")
    public ThreadPoolTaskExecutor portForwardTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        // 核心线程数（默认线程数）
        executor.setCorePoolSize(5);
        // 最大线程数
        executor.setMaxPoolSize(50);
        // 队列容量（超过核心线程数的任务会进入队列）
        executor.setQueueCapacity(200);
        // 线程名前缀
        executor.setThreadNamePrefix("port-forward-task-");
        // 拒绝策略：当线程池和队列都满时，由调用者线程执行任务
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        // 线程空闲时间（秒）：超过核心线程数的线程，空闲时间超过此值会被回收
        executor.setKeepAliveSeconds(3600 * 24);
        // 等待所有任务完成后关闭线程池
        executor.setWaitForTasksToCompleteOnShutdown(false);
        // 关闭时等待任务完成的超时时间（秒）
        executor.setAwaitTerminationSeconds(60);
        // 初始化线程池
        executor.initialize();
        return executor;
    }
}
