package com.codeages.javaskeletonserver.biz.queue.consumer;

import com.codeages.javaskeletonserver.biz.queue.handler.ConsumeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "queue.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
@RocketMQMessageListener(topic = "${queue.rocketmq.delay-topic}", consumerGroup = "${queue.rocketmq.delay-consumer-group}")
public class DelayConsumer extends BaseConsumer {
    public DelayConsumer(ConsumeHandlerRegistry handlerRegister) {
        super(handlerRegister);
    }
}
