package com.codeages.termiusplus.biz.application.job;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUnit;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.application.dto.ApplicationDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorExecDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationMonitorSearchParams;
import com.codeages.termiusplus.biz.application.service.ApplicationMonitorService;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.message.MessageService;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.extern.slf4j.Slf4j;
import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLHandshakeException;
import java.io.IOException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Component
@Slf4j
@ConditionalOnProperty(prefix = "monitor", name = "enable", havingValue = "true")
public class MonitorJob {

    @Autowired
    private ApplicationService applicationService;
    @Autowired
    private ApplicationMonitorService applicationMonitorService;
    @Autowired
    private ProxyService proxyService;

    @Autowired
    private MessageService messageService;

    private static ThreadPoolTaskExecutor executor;

    @Value("${monitor.count:3}")
    private int monitorCount;

    @Value("${monitor.debounce:5}")
    private int monitorDebounce;

    @Value("${monitor.certExpiryDays:90}")
    private int certExpiryDays;


    static {
        executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5); // 设置核心线程池大小
        executor.setMaxPoolSize(5); // 设置最大线程池大小
        executor.setQueueCapacity(5000); // 设置队列容量
        executor.setThreadNamePrefix("Monitor-"); // 设置线程名前缀
        executor.initialize();
    }


    // 每1分钟执行一次
    @SchedulerLock(name = "MonitorJob_applicationMonitor")
    @Scheduled(cron = "0 0/1 * * * ?")
    public void applicationMonitor() {
        List<ApplicationMonitorDto> applicationMonitorList = applicationMonitorService.search(
                                                                                              new ApplicationMonitorSearchParams(),
                                                                                              Pageable.unpaged()
                                                                                             )
                                                                                      .getContent();

        QueryUtils.batchQueryOneToOne(
                applicationMonitorList,
                ApplicationMonitorDto::getApplicationId,
                applicationService::findAllByIds,
                ApplicationDto::getId,
                (monitorDto, applicationDto) -> {
                    monitorDto.setApplicationContent(applicationDto.getContent());
                    monitorDto.setApplicationName(applicationDto.getName());
                    monitorDto.setMasterMobile(applicationDto.getMasterMobile());
                    monitorDto.setProxyId(applicationDto.getProxyId());
                }
                                     );

        QueryUtils.batchQueryOneToOne(
                applicationMonitorList,
                ApplicationMonitorDto::getProxyId,
                proxyService::findByIds,
                ProxyDto::getId,
                ApplicationMonitorDto::setProxy
                                     );

        Map<ApplicationMonitorDto, ApplicationMonitorExecDto> execMap = new ConcurrentHashMap<>();
        CompletableFuture<?>[] futures = new CompletableFuture[applicationMonitorList.size()];
        for (int i = 0; i < applicationMonitorList.size(); i++) {
            ApplicationMonitorDto applicationMonitor = applicationMonitorList.get(i);
            futures[i] = CompletableFuture.runAsync(() -> {
                ApplicationMonitorExecDto applicationMonitorTest = applicationMonitorService.exec(applicationMonitor);
                applicationMonitorService.updateStatusAndSendMessage(applicationMonitor, applicationMonitorTest);
                if (!applicationMonitorTest.isSuccess() || applicationMonitor.getFailureCount() != 0) {
                    execMap.put(applicationMonitor, applicationMonitorTest);
                }
            }, executor);
        }

        try {
            CompletableFuture.allOf(futures)
                             .get(30, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.error("监控任务执行超时", e);
        }

        if (CollUtil.isEmpty(execMap)) {
            return;
        }

        List<Tree<Long>> all = applicationService.findAll();

        String title = "应用监控提醒";
        String content = "\n # 以下应用有异常，请注意⚠️：\n";

        String body = execMap.entrySet()
                             .stream()
                             .filter(execDtoEntry -> {
                                 long count = execDtoEntry.getKey()
                                                          .getFailureCount();
                                 return (count <= monitorCount + monitorDebounce && count > monitorDebounce) || (execDtoEntry.getValue()
                                                                                                                             .isSuccess() && count > monitorDebounce);
                             })
                             .map(execDtoEntry -> {
                                 ApplicationMonitorDto applicationMonitorDto = execDtoEntry.getKey();
                                 ApplicationMonitorExecDto testDto = execDtoEntry.getValue();
                                 long count = applicationMonitorDto.getFailureCount();
                                 String applicationGroupPath = findApplicationGroupPath(
                                         all,
                                         applicationMonitorDto.getApplicationId()
                                                                                       );
                                 //恢复正常通知
                                 if (execDtoEntry.getValue()
                                                 .isSuccess()) {
                                     return "- [" + applicationGroupPath + "](" + applicationMonitorDto.getApplicationContent() + ")：" + count + "分钟内,恢复正常";
                                 }


                                 return "- [" + applicationGroupPath + "](" + applicationMonitorDto.getApplicationContent() + ")：" + count + "分钟无响应，" + (testDto.getRemark()
                                                                                                                                                                     .equals("failed") ? "" : "[" + testDto.getRemark() + "]");
                             })
                             .collect(Collectors.joining("\n"));

        if (StrUtil.isEmpty(body)) {
            return;
        }

        messageService.send(
                MessageSubType.MARKDOWN,
                title,
                content + body
                           );

    }

    // 每天 10点30分执行一次
    @SchedulerLock(name = "MonitorJob_checkCertExpiryDate")
    @Scheduled(cron = "0 30 10 * * ?")
    public void checkCertExpiryDate() {
        List<ApplicationDto> application = applicationService.findAllApplication();

        Date date = new Date();
        List<Pair<ApplicationDto, Long>> pairList = new ArrayList<>();
        for (ApplicationDto app : application) {
            String url = app.getContent();
            Date expiryDate = null;

            try {
                expiryDate = getCertExpiryDate(url);
            } catch (SSLHandshakeException e) {
                pairList.add(Pair.of(app, -1L));
                continue;
            } catch (Exception e) {
                log.error("获取证书到期时间失败:{}", url, e);
            }

            if (expiryDate == null) {
                continue;
            }

            long lastDay = DateUtil.between(date, expiryDate, DateUnit.DAY, false);
            log.info("应用{}:,{}证书到期时间还有{}天", app.getName(), url, lastDay);

            if (lastDay < certExpiryDays) {
                pairList.add(Pair.of(app, lastDay));
            }

        }

        if (CollUtil.isEmpty(pairList)) {
            return;
        }

        List<Tree<Long>> all = applicationService.findAll();

        pairList = pairList.stream()
                           .sorted(Comparator.comparing(Pair::getValue))
                           .collect(Collectors.toList());
        String title = "证书到期提醒";
        String content = "\n # 以下证书即将过期，请尽快处理：\n";
        String body = pairList.stream()
                              .map(pair -> {
                                  ApplicationDto app = pair.getKey();
                                  String applicationGroupPath = findApplicationGroupPath(all, app.getId());
                                  long lastDay = pair.getValue();
                                  if (lastDay <= 0) {
                                      return " - [" + applicationGroupPath + "](" + app.getContent() + ")：" + "已过期" + (lastDay == 0L ? "" : -lastDay) + "天";
                                  }

                                  return " - [" + applicationGroupPath + "](" + app.getContent() + ")" + "：" + lastDay + "天";
                              })
                              .collect(Collectors.joining("\n"));

        messageService.send(
                MessageSubType.MARKDOWN,
                title,
                content + body
                           );
    }

    private String findApplicationGroupPath(List<Tree<Long>> all, Long applicationId) {
        for (Tree<Long> tree : all) {
            if (tree.getId()
                    .equals(applicationId)) {
                return CollUtil.join(tree.getParentsName(true)
                                         .reversed(), "-");
            } else if (CollUtil.isNotEmpty(tree.getChildren())) {
                String applicationGroupPath = findApplicationGroupPath(tree.getChildren(), applicationId);
                if (CharSequenceUtil.isNotEmpty(applicationGroupPath)) {
                    return applicationGroupPath;
                }
            }
        }

        return "";
    }

    private static Date getCertExpiryDate(String urlStr) throws IOException {
        if (urlStr == null || urlStr.isEmpty()) {
            return null;
        }

        if (!urlStr.startsWith("https://")) {
            return null;
        }

        URL url = new URL(urlStr);
        HttpsURLConnection connection;
        try {
            connection = (HttpsURLConnection) url.openConnection();
            //忽略证书验证
            connection.connect();
        } catch (SSLHandshakeException e) {
            Throwable cause = e.getCause()
                               .getCause()
                               .getCause();
            return DateUtil.parse(cause.getMessage()
                                       .split(": ")[1]);
        }


        java.security.cert.Certificate[] certificates = connection.getServerCertificates();

        if (certificates.length == 0) {
            return null;
        }
        Date date = ((X509Certificate) certificates[0]).getNotAfter();
        connection.disconnect();

        return date;
    }

}
