package com.codeages.termiusplus.biz.queue.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface QueueConsumeHandler {
    /**
     * 消息类型
     */
    String type();

    /**
     * 是否记录日志
     */
    boolean log() default false;
}
