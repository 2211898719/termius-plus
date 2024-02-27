package com.codeages.javaskeletonserver.biz.server.ws.ssh;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthEditSessionDto {
    private String username;
    private String sessionId;
    private Boolean result;
}
