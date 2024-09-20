package com.codeages.termiusplus.biz.server.job;

import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
@Slf4j
public class ServerRunInfoJob {

    @Autowired
    private ServerService serverService;


    @Scheduled(cron = "0 0 0/4 *  * ?")
    public void execute() {
        serverService.syncAllServerRunInfo();
    }
}
