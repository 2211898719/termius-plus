package com.codeages.javaskeletonserver.biz.queue.handler;

import cn.hutool.core.util.ObjectUtil;

public class ConsumeHandlerContainer {

    ConsumeHandler handler;

    boolean log;

    Class<?> messageType;

    public ConsumeHandlerContainer(ConsumeHandler handler, boolean log) {
        this.handler = handler;
        this.log = log;
        this.messageType = ObjectUtil.getTypeArgument(handler);
    }

    public ConsumeHandler getHandler() {
        return handler;
    }

    public boolean isLog() {
        return log;
    }

    public Class<?> getMessageType() {
        return messageType;
    }
}
