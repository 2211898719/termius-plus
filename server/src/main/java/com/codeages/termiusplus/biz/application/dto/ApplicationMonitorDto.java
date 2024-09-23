package com.codeages.termiusplus.biz.application.dto;

import com.codeages.termiusplus.biz.application.enums.ApplicationMonitorTypeEnum;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorDto {

    private Long id;

    private String applicationName;

    private String applicationContent;
    private String masterMobile;

    private Long applicationId;

    private ApplicationMonitorTypeEnum type;

    private String config;

    private String remark;
    private ProxyDto proxy;
    private Long proxyId;

    private Long failureCount;
    private Date failureTime;
    private String responseResult;
    private Long responseTime;
}

