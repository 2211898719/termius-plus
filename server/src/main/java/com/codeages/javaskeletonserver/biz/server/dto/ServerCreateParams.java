package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerCreateParams {
    private Long groupId;
    private String name;
    private String username;
    private String ip;
    private Long port;
    private Long sort;
    private Boolean autoSudo;
    private Boolean isGroup;
    private Long parentId;
    private String password;
    private String key;
    private Long proxyId;
}

