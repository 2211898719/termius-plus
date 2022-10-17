package com.codeages.javaskeletonserver.biz.queue.handler;

public interface ConsumeHandler<T> {
    void handle(T payload);
}
