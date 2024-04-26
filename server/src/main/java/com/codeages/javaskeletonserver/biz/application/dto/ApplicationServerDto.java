package com.codeages.javaskeletonserver.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationServerDto {

    private Long id;


    private Long applicationId;


    private Long serverId;


    private String tag;


    private String remark;
}

