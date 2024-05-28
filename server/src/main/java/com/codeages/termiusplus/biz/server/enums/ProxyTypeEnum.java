package com.codeages.termiusplus.biz.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.net.Proxy;

@Getter
@AllArgsConstructor
public enum ProxyTypeEnum {
    HTTP(3L, Proxy.Type.HTTP),
    SOCKET4(1L, Proxy.Type.HTTP),
    SOCKET5(2L, Proxy.Type.SOCKS),
    ;

    private final Long code;

    private final Proxy.Type Type;
}
