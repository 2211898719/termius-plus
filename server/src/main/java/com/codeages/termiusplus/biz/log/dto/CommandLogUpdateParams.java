package com.codeages.termiusplus.biz.log.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandLogUpdateParams {
    private Long id;
    private Long userId;
    private String sessionId;
    private Long serverId;
    private String commandData;
}

