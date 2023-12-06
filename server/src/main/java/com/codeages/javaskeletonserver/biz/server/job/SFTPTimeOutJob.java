package com.codeages.javaskeletonserver.biz.server.job;

import com.codeages.javaskeletonserver.biz.server.context.SFTPContext;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class SFTPTimeOutJob {

    // 每半小时清理一次超时的连接
     @Scheduled(cron = "0 0/30 * * * ?")
    public void clearTimeOutSFTP() {
         SFTPContext.SFTP_POOL.forEach((k, v) -> {
             if (v.getSftp() == null) {
                 SFTPContext.SFTP_POOL.remove(k);
                 log.info("SFTP连接已失效：{}", k);
                 return;
             }

             if (System.currentTimeMillis() - v.getTime() > 30 * 60 * 1000) {
                 try {
                     v.getSftp().close();
                 } catch (IOException e) {
                     log.error("关闭sftp连接失败", e);
                 }
                 SFTPContext.SFTP_POOL.remove(k);
                 log.info("SFTP连接已失效：{}", k);
             }
         });
     }

}
