package com.codeages.javaskeletonserver.biz.log.dto;

import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandLogDto {
    private Long id;
    private Long userId;
    private String userName;
    private String sessionId;
    private Long serverId;
    private String serverName;
    private String commandData;
}

