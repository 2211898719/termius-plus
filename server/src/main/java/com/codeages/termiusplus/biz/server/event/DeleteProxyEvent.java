package com.codeages.termiusplus.biz.server.event;


import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class DeleteProxyEvent extends ApplicationEvent {

    private ProxyDto proxyDto;

    public DeleteProxyEvent(Object source, ProxyDto proxyDto) {
        super(source);
        this.proxyDto = proxyDto;
    }
}
