package com.codeages.javaskeletonserver.biz.job.service.impl;

import cn.hutool.extra.spring.SpringUtil;
import com.codeages.javaskeletonserver.biz.job.RunMvelJob;
import com.codeages.javaskeletonserver.biz.job.dto.MvelCronCreateDto;
import com.codeages.javaskeletonserver.biz.job.dto.MySSHClient;
import com.codeages.javaskeletonserver.biz.job.service.QuartzService;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.github.jaemon.dinger.DingerSender;
import com.github.jaemon.dinger.core.entity.DingerRequest;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.connection.channel.direct.Session;
import org.mvel2.MVEL;
import org.mvel2.integration.impl.DefaultLocalVariableResolverFactory;
import org.quartz.*;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class QuartzServiceImpl implements QuartzService {

    @Autowired
    private Scheduler scheduler;

    @SneakyThrows
    public List<JobDetail> getCurrentlyExecutingJobs() {
        List<JobDetail> jobDetails = new ArrayList<>();
        for (JobKey jobKey : scheduler.getJobKeys(GroupMatcher.anyGroup())) {
            jobDetails.add(scheduler.getJobDetail(jobKey));
        }

        return jobDetails;
    }

    @Override
    public void execJob(JobDataMap jobDataMap) {
        String mvelScript = String.valueOf(jobDataMap.get("mvelScript"));
        Long serverId = Long.valueOf(String.valueOf(jobDataMap.get("serverId")));
        ServerService serverService = SpringUtil.getBean(ServerService.class);
        SSHClient sshClient = serverService.createSSHClient(serverId);
        ServerDto serverDto = serverService.findById(serverId);
        log.info("服务器id：{}，mvel脚本: {}", serverId, mvelScript);
        DefaultLocalVariableResolverFactory variableResolverFactory = new DefaultLocalVariableResolverFactory();

        MVEL.eval(
                mvelScript,
                Map.of(
                        "dingerSender", SpringUtil.getBean(DingerSender.class),
                        "MessageSubType", MessageSubType.class,
                        "DingerRequest", DingerRequest.class,
                        "session", new MySSHClient(sshClient),
                        "server", serverDto
                ),
                variableResolverFactory
        );
    }



    @SneakyThrows
    public void createCronJob(MvelCronCreateDto dto) {
        JobDetail jobDetail = JobBuilder.newJob(RunMvelJob.class)
                                        .setJobData(new JobDataMap(Map.of(
                                                "mvelScript", dto.getMvelScript(),
                                                "serverId", dto.getServerId(),
                                                "cronExpression", dto.getCronExpression()
                                        )))
                                        .withIdentity(dto.getJobName(), dto.getJobGroup())
                                        .build();

        CronTrigger trigger = TriggerBuilder.newTrigger()
                                            .withIdentity(dto.getJobName(), dto.getJobGroup())
                                            .withSchedule(CronScheduleBuilder.cronSchedule(dto.getCronExpression()))
                                            .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }

    @SneakyThrows
    public void deleteJob(String jobName, String jobGroup) {
        JobKey jobKey = JobKey.jobKey(jobName, jobGroup);
        scheduler.deleteJob(jobKey);
    }

}
