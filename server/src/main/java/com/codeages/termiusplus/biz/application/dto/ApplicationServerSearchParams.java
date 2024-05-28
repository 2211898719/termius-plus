package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationServerSearchParams {


    private Long applicationId;


    private Long serverId;


    private String tag;


    private String remark;
}

