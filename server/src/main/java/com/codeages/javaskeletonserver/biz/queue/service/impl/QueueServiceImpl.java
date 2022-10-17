package com.codeages.javaskeletonserver.biz.queue.service.impl;

import com.codeages.javaskeletonserver.biz.queue.service.QueueService;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.support.MessageBuilder;

public class QueueServiceImpl implements QueueService {

    private final String normalTopic;

    private final String delayTopic;

    private final String orderlyTopic;

    final RocketMQTemplate rocketMQTemplate;

    final long SEND_TIMEOUT = 3000;

    private static final Logger log = LoggerFactory.getLogger(QueueServiceImpl.class);

    public QueueServiceImpl(RocketMQTemplate rocketMQTemplate, String normalTopic, String delayTopic, String orderlyTopic) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.normalTopic = normalTopic;
        this.delayTopic = delayTopic;
        this.orderlyTopic = orderlyTopic;
    }

    @Override
    public <T> void send(String type, T payload) {
        send(type, payload, null);
    }

    @Override
    public <T> void send(String type, T payload, String tag) {
        var message = MessageBuilder
                .withPayload(payload)
                .setHeader("type", type)
                .build();
        var sendOrderlyRes = rocketMQTemplate.syncSend(makeDestination(normalTopic, tag), message, SEND_TIMEOUT);
    }

    @Override
    public <T> void sendDelay(String type, T payload, int delayLevel) {
        sendDelay(type, payload, delayLevel, null);
    }

    @Override
    public <T> void sendDelay(String type, T payload, int delayLevel, String tag) {
        var message = MessageBuilder
                .withPayload(payload)
                .setHeader("type", type)
                .build();
        rocketMQTemplate.syncSend(makeDestination(delayTopic, tag), message, SEND_TIMEOUT, delayLevel);
    }

    @Override
    public <T> void sendOrderly(String type, T payload, String sharedKey) {
        sendOrderly(type, payload, sharedKey, null);
    }

    @Override
    public <T> void sendOrderly(String type, T payload, String sharedKey, String tag) {
        var message = MessageBuilder
                .withPayload(payload)
                .setHeader("type", type)
                .build();
        var sendOrderlyRes = rocketMQTemplate.syncSendOrderly(makeDestination(orderlyTopic, tag), message, sharedKey, SEND_TIMEOUT);
    }

    private String makeDestination(String topic, String tag) {
        return tag == null ? topic : (topic + ":" + tag);
    }
}
