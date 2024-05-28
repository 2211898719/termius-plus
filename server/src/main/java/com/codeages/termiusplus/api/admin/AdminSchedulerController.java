package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.scheduler.service.SchedulerService;
import com.codeages.termiusplus.common.OkResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api-admin/scheduler")
public class AdminSchedulerController {

    private final SchedulerService schedulerService;

    public AdminSchedulerController(SchedulerService schedulerService) {
        this.schedulerService = schedulerService;
    }


    @PostMapping("/resetSystemJobs")
    public OkResponse resetSystemJobs() {
        schedulerService.resetSystemJobs();
        return OkResponse.TRUE;
    }
}
