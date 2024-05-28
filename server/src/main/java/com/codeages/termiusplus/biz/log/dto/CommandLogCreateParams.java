package com.codeages.termiusplus.biz.log.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandLogCreateParams {
    private Long userId;
    private String sessionId;
    private Long serverId;
}

