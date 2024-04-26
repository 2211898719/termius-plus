package com.codeages.javaskeletonserver.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationMonitorSearchParams {


    private Long applicationId;


    private String type;


    private String config;


    private String remark;
}

