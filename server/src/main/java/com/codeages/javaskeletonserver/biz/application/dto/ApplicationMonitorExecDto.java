package com.codeages.javaskeletonserver.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorExecDto {

    private String request;
    private String response;
    private boolean success;
    private String message;
    private Long responseTime;
}
