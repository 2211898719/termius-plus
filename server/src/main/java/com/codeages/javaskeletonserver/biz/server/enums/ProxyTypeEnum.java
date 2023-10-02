package com.codeages.javaskeletonserver.biz.server.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ProxyTypeEnum {
    HTTP(3L),
    SOCKET4(1L),
    SOCKET5(2L),
    ;

    private final Long code;
}
