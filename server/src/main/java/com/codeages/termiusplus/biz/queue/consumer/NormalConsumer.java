package com.codeages.termiusplus.biz.queue.consumer;

import com.codeages.termiusplus.biz.queue.handler.ConsumeHandlerRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnProperty(value = "queue.mock", havingValue = "false", matchIfMissing = true)
@Slf4j
@RocketMQMessageListener(topic = "${queue.rocketmq.normal-topic}", consumerGroup = "${queue.rocketmq.normal-consumer-group}")
public class NormalConsumer extends BaseConsumer {

    public NormalConsumer(ConsumeHandlerRegistry handlerRegister) {
        super(handlerRegister);
    }
}
