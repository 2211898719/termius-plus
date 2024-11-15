package com.codeages.termiusplus.biz.application.listener;

import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.application.service.ApplicationService;
import com.codeages.termiusplus.biz.server.event.DeleteProxyEvent;
import com.codeages.termiusplus.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DeleteProxyEventApplicationListener implements ApplicationListener<DeleteProxyEvent> {

    @Autowired
    private ApplicationService applicationServer;

    @Override
    public void onApplicationEvent(DeleteProxyEvent event) {
        if (applicationServer.existByProxyId(event.getProxyDto().getId())) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT,"尚有应用使用该代理");
        }
    }
}
