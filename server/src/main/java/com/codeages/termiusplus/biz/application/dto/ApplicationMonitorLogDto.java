package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorLogDto {
    private Long id;
    private Long applicationId;
    private String applicationName;
    private String applicationContent;
    private Date date;
}

