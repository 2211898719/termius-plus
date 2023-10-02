package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerServiceMonitorDto {
    private Long id;
    private Long serverId;
    private String name;
    private Long port;
}

