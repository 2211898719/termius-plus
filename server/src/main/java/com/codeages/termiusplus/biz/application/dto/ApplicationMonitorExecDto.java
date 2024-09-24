package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorExecDto {

    private boolean res;
    private String request;
    private String response;
    private boolean success;
    private String message;
    private String remark;
    private Long responseTime;
}

