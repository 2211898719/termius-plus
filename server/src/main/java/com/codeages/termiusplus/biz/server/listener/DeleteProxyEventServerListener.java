package com.codeages.termiusplus.biz.server.listener;

import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.event.DeleteProxyEvent;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.exception.AppException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationListener;
import org.springframework.stereotype.Component;

@Component
public class DeleteProxyEventServerListener implements ApplicationListener<DeleteProxyEvent> {

    @Autowired
    private ServerService serverService;

    @Override
    public void onApplicationEvent(DeleteProxyEvent event) {
        ProxyDto proxyDto = event.getProxyDto();
        if (serverService.existServerByProxyId(proxyDto.getId())) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, "尚有服务器或组在使用此代理");
        }
    }
}
