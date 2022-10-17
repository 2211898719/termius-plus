package com.codeages.javaskeletonserver.biz.queue.service.impl;

import com.codeages.javaskeletonserver.biz.queue.service.QueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MockQueueServiceImpl implements QueueService {

    private static final Logger log = LoggerFactory.getLogger(MockQueueServiceImpl.class);

    @Override
    public <T> void send(String type, T payload) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }

    @Override
    public <T> void send(String type, T payload, String tag) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }

    @Override
    public <T> void sendDelay(String type, T payload, int delayLevel) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }

    @Override
    public <T> void sendDelay(String type, T payload, int delayLevel, String tag) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }

    @Override
    public <T> void sendOrderly(String type, T payload, String sharedKey) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }

    @Override
    public <T> void sendOrderly(String type, T payload, String sharedKey, String tag) {
        log.warn("queue is mocked, don't send (type: {}, payload: {})", type, payload);
    }
}
