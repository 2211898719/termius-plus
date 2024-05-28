package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.job.dto.MvelCronCreateDto;
import com.codeages.termiusplus.biz.job.service.QuartzService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import org.quartz.JobDetail;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api-admin/quartz")
public class QuartzController {
    private final QuartzService quartzService;
    private final ServerService serverService;

    public QuartzController(QuartzService quartzService, ServerService serverService) {
        this.quartzService = quartzService;
        this.serverService = serverService;
    }

    @GetMapping("/list")
    public List<MvelCronCreateDto> list() {
        List<JobDetail> currentlyExecutingJobs = quartzService.getCurrentlyExecutingJobs();
        return currentlyExecutingJobs.stream().map(jobDetail -> new MvelCronCreateDto(
                jobDetail.getKey().getName(),
                jobDetail.getKey().getGroup(),
                (List<Long>) jobDetail.getJobDataMap().get("serverIds"),
                serverService.findByIdIn((List<Long>) jobDetail.getJobDataMap().get("serverIds")),
                (List<String>) jobDetail.getJobDataMap().get("params"),
                jobDetail.getJobDataMap().getString("mvelScript"),
                jobDetail.getJobDataMap().getString("cronExpression")
        )).collect(Collectors.toList());
    }

    @PostMapping("/create")
    public boolean createCronJob(@RequestBody MvelCronCreateDto dto) {
        quartzService.createCronJob(dto);

        return true;
    }

    @PostMapping("/delete")
    public boolean deleteJob(@RequestBody MvelCronCreateDto dto) {
        quartzService.deleteJob(dto.getJobName(), dto.getJobGroup());
        return true;
    }

    @PostMapping("/update")
    public boolean updateJob(@RequestBody MvelCronCreateDto dto) {
        quartzService.deleteJob(dto.getJobName(), dto.getJobGroup());
        quartzService.createCronJob(dto);
        return true;
    }


}
