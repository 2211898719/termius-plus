package com.codeages.termiusplus.biz.application.dto;

import com.codeages.termiusplus.biz.application.enums.ApplicationMonitorTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorUpdateParams {

    private Long id;

    @NotNull(message = "应用ID不能为空")

    private Long applicationId;

    @NotNull(message = "监控类型不能为空")

    private ApplicationMonitorTypeEnum type;

    @NotNull(message = "监控配置不能为空")

    private String config;


    private String remark;

    private Long failureCount;
    private Date failureTime;
    private String responseResult;
    private Long responseTime;
}

