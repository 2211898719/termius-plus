package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.queue.example.ExampleMessage;
import com.codeages.javaskeletonserver.biz.queue.service.QueueService;
import com.codeages.javaskeletonserver.biz.scheduler.service.SchedulerService;
import com.codeages.javaskeletonserver.common.OkResponse;
import org.quartz.SchedulerException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api-admin/queue")
public class AdminQueueController {

    private final QueueService queueService;

    public AdminQueueController(QueueService queueService) {
        this.queueService = queueService;
    }

    
    @PostMapping("/test")
    public OkResponse test() {
        var message = new ExampleMessage();
        message.setId(1000);
        message.setText("hello, world!");

        queueService.send("example", message);

        return OkResponse.TRUE;
    }
}
