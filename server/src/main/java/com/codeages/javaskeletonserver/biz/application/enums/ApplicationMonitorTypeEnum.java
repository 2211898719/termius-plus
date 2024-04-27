package com.codeages.javaskeletonserver.biz.application.enums;

import com.codeages.javaskeletonserver.biz.application.config.ApplicationMonitorRequestConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ApplicationMonitorTypeEnum {
    REQUEST(ApplicationMonitorRequestConfig.class)

    ;

    @Getter
    private final Class<?> configClass;


}
