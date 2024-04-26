package com.codeages.javaskeletonserver.biz.application.dto;

import com.codeages.javaskeletonserver.biz.application.enums.ApplicationMonitorTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorCreateParams {

    @NotNull(message = "应用ID不能为空")

    private Long applicationId;

    @NotNull(message = "监控类型不能为空")

    private ApplicationMonitorTypeEnum type;

    @NotNull(message = "监控配置不能为空")

    private String config;


    private String remark;
}

