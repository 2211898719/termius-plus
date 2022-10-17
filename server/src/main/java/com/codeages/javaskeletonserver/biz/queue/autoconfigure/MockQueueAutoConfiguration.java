package com.codeages.javaskeletonserver.biz.queue.autoconfigure;

import com.codeages.javaskeletonserver.biz.queue.service.QueueService;
import com.codeages.javaskeletonserver.biz.queue.service.impl.MockQueueServiceImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(value = "queue.mock", havingValue = "true")
public class MockQueueAutoConfiguration {

    @Bean
    public QueueService queueService() {
        return new MockQueueServiceImpl();
    }

}
