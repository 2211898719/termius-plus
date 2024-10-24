package com.codeages.termiusplus.biz.server.job;

import com.codeages.termiusplus.biz.server.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ProxySyncJob {
    @Autowired
    private ProxyService proxyService;

    @PostConstruct
    public void init() {
        log.info("init ProxySyncJob");
        proxyService.syncProxyOpen();
    }

    // 每5分钟同步一次
    @Scheduled(cron = "0 */5 * * * *")
    @Async
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
