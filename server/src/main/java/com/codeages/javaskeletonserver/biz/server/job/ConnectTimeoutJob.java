package com.codeages.javaskeletonserver.biz.server.job;

import com.codeages.javaskeletonserver.biz.server.context.ServerContext;
import com.codeages.javaskeletonserver.biz.server.dto.SFTPBean;
import com.codeages.javaskeletonserver.biz.server.ws.ssh.SshHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@Slf4j
public class ConnectTimeoutJob {

    // 每半小时清理一次超时的连接
    @Scheduled(cron = "0 0/10 * * * ?")
    public void clearTimeOutSFTP() {
        for (String k : ServerContext.SFTP_POOL.keySet()) {
            SFTPBean v = ServerContext.SFTP_POOL.get(k);
            if (v.getSftp() == null) {
                ServerContext.SFTP_POOL.remove(k);
                log.info("SFTP连接已失效：{}", k);
                return;
            }

            if (System.currentTimeMillis() - v.getTime() > 30 * 60 * 1000) {
                try {
                    v.getSftp().close();
                } catch (IOException e) {
                    log.error("关闭sftp连接失败", e);
                }
                ServerContext.SFTP_POOL.remove(k);
                log.info("SFTP连接已失效：{}", k);
            }
        }
     }


    @Scheduled(cron = "0 0/10 * * * ?")
    public void clearTimeOutSsh() {
        log.info("开始清理超时的SSH连接");
        for (String k : ServerContext.SSH_POOL.keySet()) {
            SshHandler.HandlerItem v = ServerContext.SSH_POOL.get(k);
            if (!v.isOpen()) {
                v.close();
                ServerContext.SSH_POOL.remove(k);
                log.info("SSH连接已失效：{}", k);
            }
        }
    }

}
