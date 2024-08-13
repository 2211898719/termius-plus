package com.codeages.termiusplus.biz.server.job;

import cn.hutool.core.date.DateUnit;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.biz.server.dto.SFTPBean;
import com.codeages.termiusplus.ws.ssh.SshHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class ConnectTimeoutJob {

    // 每半小时清理一次超时的连接
    @Scheduled(cron = "0 0 0/23 *  * ?")
    public void clearTimeOutSFTP() {
        for (String k : ServerContext.SFTP_POOL.keySet()) {
            SFTPBean v = ServerContext.SFTP_POOL.get(k);
            if (v.getSftp() == null) {
                ServerContext.SFTP_POOL.remove(k);
                log.info("SFTP连接已失效：{}", k);
                return;
            }

            if (System.currentTimeMillis() - v.getTime() > 24 * 60 * 60 * 1000 && !v.isActive()) {
                try {
                    v.getSftp().close();
                } catch (IOException e) {
                    log.error("关闭sftp连接失败", e);
                }
                ServerContext.SFTP_POOL.remove(k);
                log.info("SFTP连接已失效了：{}", k);
            }
        }
    }


    @Scheduled(cron = "0 0 0/4 *  * ?")
    public void clearTimeOutSsh() {
        log.info("开始清理超时的SSH连接");
        List<String> key = new ArrayList<>();
        for (Map.Entry<String, SshHandler.HandlerItem> entry : ServerContext.SSH_POOL.entrySet()) {
            long diffTime = System.currentTimeMillis() - entry.getValue().getLastActiveTime();

            if (!entry.getValue().isOpen() || diffTime > DateUnit.DAY.getMillis()) {
                entry.getValue().close();
                key.add(entry.getKey());
                log.info("SSH连接已失效：{}", entry.getValue().getServerId());
            }
        }

        key.forEach(ServerContext.SSH_POOL::remove);
    }

}
