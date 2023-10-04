package com.codeages.javaskeletonserver.biz.server.enums;

import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProxyTypeEnum {
    HTTP(3L, ProxyHTTP.class),
    SOCKET4(1L, ProxySOCKS4.class),
    SOCKET5(2L, ProxySOCKS5.class),
    ;

    private final Long code;

    private final Class<? extends Proxy> proxyClass;
}
