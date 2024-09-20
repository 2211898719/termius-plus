package com.codeages.termiusplus.biz.server.job;

import com.codeages.termiusplus.biz.server.service.ProxyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.concurrent.CompletableFuture;

@Component
@Slf4j
public class ProxySyncJob {
    @Autowired
    private ProxyService proxyService;

//    @PostConstruct
//    public void init() {
//        log.info("ProxySyncJob init");
//        CompletableFuture.runAsync(() -> {
//            proxyService.syncClashProxy();
//            log.info("ProxySyncJob finished");
//        });
//    }
//
//    // 每周一凌晨2点执行
//    @Scheduled(cron = "0 0 2 ? * MON")
//    @Async
//    public void clearTimeOutSFTP() {
//        proxyService.syncClashProxy();
//    }
}
