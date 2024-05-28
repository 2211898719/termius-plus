package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.queue.example.ExampleMessage;
import com.codeages.termiusplus.biz.queue.service.QueueService;
import com.codeages.termiusplus.common.OkResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
