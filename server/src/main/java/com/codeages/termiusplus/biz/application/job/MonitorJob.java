package com.codeages.termiusplus.biz.application.job;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.application.dto.ApplicationDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorExecDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorSearchParams;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.github.jaemon.dinger.DingerSender;
import com.github.jaemon.dinger.core.entity.DingerRequest;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
public class MonitorJob {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationMonitorService applicationMonitorService;

    @Autowired
    private DingerSender dingerSender;

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

    // 每天 10点30分执行一次
    @Scheduled(cron = "0 30 10 * * ?")
    public void checkCertExpiryDate() {
        List<ApplicationDto> application = applicationService.findAllApplication();
        for (ApplicationDto app : application) {
            String url = app.getContent();

            Date expiryDate = null;

            try {
                expiryDate = getCertExpiryDate(url);
            } catch (SSLHandshakeException e) {
                dingerSender.send(
                        MessageSubType.TEXT,
                        DingerRequest.request(
                                "应用证书验证失败，可能为dns解析错误或证书已经过期，请尽快处理，应用名称：" + app.getName() + "，应用内容：" + app.getContent(),
                                StrUtil.isEmpty(app.getMasterMobile()) ? null : List.of(app.getMasterMobile())
                        )
                );
                ThreadUtil.sleep(2, TimeUnit.SECONDS);
                log.error("获取证书到期时间失败:{}", url, e);
                continue;
            } catch (Exception e) {
                log.error("获取证书到期时间失败:{}", url, e);
            }

            if (expiryDate == null) {
                continue;
            }

            long lastDay = DateUtil.betweenDay(expiryDate, new Date(), false);
            log.info("应用{}:,{}证书到期时间还有{}天", app.getName(), url, lastDay);

            if (lastDay < 30) {
                dingerSender.send(
                        MessageSubType.TEXT,
                        DingerRequest.request(
                                "应用证书即将过期，还有" + lastDay + "天，请尽快处理，应用名称：" + app.getName() + "，应用内容：" + app.getContent(),
                                StrUtil.isEmpty(app.getMasterMobile()) ? null : List.of(app.getMasterMobile())
                        )
                );
                ThreadUtil.sleep(2, TimeUnit.SECONDS);
            }


        }
    }

    private static Date getCertExpiryDate(String urlStr) throws IOException {
        if (urlStr == null || urlStr.isEmpty()) {
            return null;
        }

        if (!urlStr.startsWith("https://")) {
            return null;
        }

        URL url = new URL(urlStr);
        HttpsURLConnection connection = (HttpsURLConnection) url.openConnection();
        //忽略证书验证
        connection.connect();

        java.security.cert.Certificate[] certificates = connection.getServerCertificates();

        if (certificates.length == 0) {
            return null;
        }
        Date date = ((X509Certificate) certificates[0]).getNotAfter();
        connection.disconnect();

        return date;
    }

}
