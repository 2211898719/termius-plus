package com.codeages.javaskeletonserver.biz.queue.service;

/**
 * 消息队列服务
 */
public interface QueueService {
    /**
     * 发送普通消息，消息被消费时是无序的
     * @param type 消息类型
     * @param payload 消息载荷
     * @param <T> 消息体对象类型
     */
    <T> void send(String type, T payload);

    /**
     * 发送普通消息，消息被消费时是无序的
     * @param type 消息类型
     * @param payload 消息载荷
     * @param tag 消息标签
     * @param <T> 消息体对象类型
     */
    <T> void send(String type, T payload, String tag);

    /**
     * 发送延迟普通消息，消息被消费时是无序的
     * @param type 消息类型
     * @param payload 消息载荷
     * @param delayLevel 参见 DelayLevel 类
     * @param <T> 消息体对象类型
     */
    <T> void sendDelay(String type, T payload, int delayLevel);

    /**
     * 发送延迟普通消息，消息被消费时是无序的
     * @param type 消息类型
     * @param payload 消息载荷
     * @param delayLevel 参见 DelayLevel 类
     * @param tag 消息标签
     * @param <T> 消息体对象类型
     */
    <T> void sendDelay(String type, T payload, int delayLevel, String tag);

    /**
     * 发送顺序消息，消息按 sharedKey 分区有序消费
     * @param type 消息类型
     * @param payload 消息载荷
     * @param sharedKey 分区Key
     * @param <T> 消息体对象类型
     */
    <T> void sendOrderly(String type, T payload, String sharedKey);

    /**
     * 发送顺序消息，消息按 sharedKey 分区有序消费
     * @param type 消息类型
     * @param payload 消息载荷
     * @param sharedKey 分区Key
     * @param tag 消息标签
     * @param <T> 消息体对象类型
     */
    <T> void sendOrderly(String type, T payload, String sharedKey, String tag);
}
