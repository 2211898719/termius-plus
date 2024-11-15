package com.codeages.termiusplus.biz.message;

import com.github.jaemon.dinger.DingerSender;
import com.github.jaemon.dinger.core.entity.DingerRequest;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Service;

@Service
@ConditionalOnExpression("!${spring.dingtalk.enabled:false}")
public class MessageServiceDingerImpl implements MessageService {

    @Autowired
    private DingerSender dingerSender;


    @Override
    public void send(MessageSubType messageType, String title, String message) {
        dingerSender.send(
                messageType,
                DingerRequest.request(
                        message,
                        title
                                     )
                         );
    }
}
