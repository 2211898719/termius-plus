package com.codeages.termiusplus.biz.queue.handler;

public interface ConsumeHandler<T> {
    void handle(T payload);
}
