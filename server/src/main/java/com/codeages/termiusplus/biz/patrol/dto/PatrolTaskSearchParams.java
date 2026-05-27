package com.codeages.termiusplus.biz.patrol.dto;

import lombok.Data;

@Data
public class PatrolTaskSearchParams {
    private Long scriptId;
    private Long serverId;
    private String status;
}
