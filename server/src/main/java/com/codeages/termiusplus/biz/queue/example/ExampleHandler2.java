package com.codeages.termiusplus.biz.queue.example;

import com.codeages.termiusplus.biz.queue.annotation.QueueConsumeHandler;
import com.codeages.termiusplus.biz.queue.handler.ConsumeHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@QueueConsumeHandler(type = "example", log = true)
public class ExampleHandler2 implements ConsumeHandler<ExampleMessage> {

    @Override
    public void handle(ExampleMessage payload) {
        log.info("ExampleConsumeHandler2 {}", payload);
    }
}
