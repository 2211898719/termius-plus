package com.codeages.javaskeletonserver.biz.log.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandLogDto {
    private Long id;
    private String sessionId;
    private Long serverId;
    private String commandData;
}
