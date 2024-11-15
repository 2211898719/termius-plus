package com.codeages.termiusplus.biz.server.job;

import com.codeages.termiusplus.biz.server.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ProxySyncJob {
    @Autowired
    private ProxyService proxyService;

    // 每5分钟同步一次
    @Async
    @Scheduled(cron = "0 */5 * * * *")
    @SchedulerLock(name = "ProxySyncJob_clearTimeOutSFTP")
    public void clearTimeOutSFTP() {
        CompletableFuture.runAsync(() -> {
            try {
                proxyService.syncProxyOpen();
            } catch (Exception e) {
                log.error("sync proxy error", e);
            }
        });
    }
}
