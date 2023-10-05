package com.codeages.javaskeletonserver.biz.server.enums;

import cn.hutool.core.lang.reflect.MethodHandleUtil;
import com.jcraft.jsch.Proxy;
import com.jcraft.jsch.ProxyHTTP;
import com.jcraft.jsch.ProxySOCKS4;
import com.jcraft.jsch.ProxySOCKS5;
import lombok.Getter;
import lombok.SneakyThrows;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;

@Getter
public enum ProxyTypeEnum {
    HTTP(3L, ProxyHTTP.class),
    SOCKET4(1L, ProxySOCKS4.class),
    SOCKET5(2L, ProxySOCKS5.class),
    ;

    private final Long code;

    private final Constructor<? extends Proxy> constructor;

    private final MethodHandle setUserPasswd;

    @SneakyThrows
    ProxyTypeEnum(Long code, Class<? extends Proxy> proxyClass) {
        this.code = code;
        this.constructor = proxyClass.getDeclaredConstructor(String.class, int.class);
        this.setUserPasswd = MethodHandleUtil.findMethod(
                proxyClass,
                "setUserPasswd",
                MethodType.methodType(void.class, String.class, String.class)
        );
    }
}
