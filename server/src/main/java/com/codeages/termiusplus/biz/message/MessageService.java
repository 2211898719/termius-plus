package com.codeages.termiusplus.biz.message;

import com.github.jaemon.dinger.core.entity.enums.MessageSubType;

public interface MessageService {

    void send(MessageSubType messageType, String title, String message);
}
