package com.codeages.javaskeletonserver.biz.server.job;

import com.codeages.javaskeletonserver.biz.server.context.SFTPContext;
import com.codeages.javaskeletonserver.biz.server.service.impl.SFTPServiceImpl;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class SFTPTimeOutJob {

    // 每半小时清理一次超时的连接
     @Scheduled(cron = "0 0/30 * * * ?")
    public void clearTimeOutSFTP() {
         SFTPContext.SFTP_POOL.forEach((k, v) -> {
             if (v.getSftp() == null) {
                 SFTPContext.SFTP_POOL.remove(k);
             }

             if (System.currentTimeMillis() - v.getTime() > 30 * 60 * 1000) {
                 v.getSftp().close();
                 SFTPContext.SFTP_POOL.remove(k);
             }
         });
     }

}
