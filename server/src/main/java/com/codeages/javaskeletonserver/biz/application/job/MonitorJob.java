package com.codeages.javaskeletonserver.biz.application.job;

import com.codeages.javaskeletonserver.biz.application.dto.ApplicationDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorExecDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorSearchParams;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationMonitorService;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationService;
import com.codeages.javaskeletonserver.biz.util.QueryUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class MonitorJob {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationMonitorService applicationMonitorService;

    private static ThreadPoolTaskExecutor executor;

    static {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10); // 设置核心线程池大小
        executor.setMaxPoolSize(10); // 设置最大线程池大小
        executor.setQueueCapacity(5000); // 设置队列容量
        executor.setThreadNamePrefix("Monitor-"); // 设置线程名前缀
        executor.initialize();
    }


    // 每1分钟执行一次
    @Scheduled(cron = "0 0/1 * * * ?")
    public void applicationMonitor() {
        List<ApplicationMonitorDto> applicationMonitorList = applicationMonitorService.search(
                new ApplicationMonitorSearchParams(),
                Pageable.unpaged()
        ).getContent();

        QueryUtils.batchQueryOneToOne(
                applicationMonitorList,
                ApplicationMonitorDto::getApplicationId,
                applicationService::findAllByIds,
                ApplicationDto::getId,
                (monitorDto, applicationDto) -> {
                    monitorDto.setApplicationContent(applicationDto.getContent());
                    monitorDto.setApplicationName(applicationDto.getName());
                    monitorDto.setApplicationName(applicationDto.getName());
                    monitorDto.setMasterMobile(applicationDto.getMasterMobile());
                }
        );

        for (ApplicationMonitorDto applicationMonitor : applicationMonitorList) {
            CompletableFuture.runAsync(() -> {
                ApplicationMonitorExecDto applicationMonitorTest = applicationMonitorService.exec(applicationMonitor);
                applicationMonitorService.updateStatusAndSendMessage(applicationMonitor, applicationMonitorTest);
            }, executor);
        }
    }

}
