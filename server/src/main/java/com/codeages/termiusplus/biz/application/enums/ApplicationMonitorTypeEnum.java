package com.codeages.termiusplus.biz.application.enums;

import com.codeages.termiusplus.biz.application.config.ApplicationMonitorRequestConfig;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
public enum ApplicationMonitorTypeEnum {
    REQUEST(ApplicationMonitorRequestConfig.class)

    ;

    @Getter
    private final Class<?> configClass;


}
