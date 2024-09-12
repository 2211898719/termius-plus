package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorLogCountDto {
    private Long applicationId;
    private String applicationName;
    private String applicationContent;
    private Long errorSeconds;
}

