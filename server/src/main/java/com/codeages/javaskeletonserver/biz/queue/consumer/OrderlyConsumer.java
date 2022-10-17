package com.codeages.javaskeletonserver.biz.queue.consumer;

import com.codeages.javaskeletonserver.biz.queue.handler.ConsumeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.ConsumeMode;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "queue.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
@RocketMQMessageListener(topic = "${queue.rocketmq.orderly-topic}", consumerGroup = "${queue.rocketmq.orderly-consumer-group}", consumeMode = ConsumeMode.ORDERLY)
public class OrderlyConsumer extends BaseConsumer {

    public OrderlyConsumer(ConsumeHandlerRegistry handlerRegister) {
        super(handlerRegister);
    }
}
