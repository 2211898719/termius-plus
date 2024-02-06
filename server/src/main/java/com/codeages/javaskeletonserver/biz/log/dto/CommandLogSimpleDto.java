package com.codeages.javaskeletonserver.biz.log.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandLogSimpleDto {
    private Long id;
    private Long userId;
    private String userName;
    private String sessionId;
    private Long serverId;
    private String serverName;
}

