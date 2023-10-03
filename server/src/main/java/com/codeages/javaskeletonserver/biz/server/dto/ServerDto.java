package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerDto {
    private Long id;
    private Boolean isGroup;
    private String firstCommand;
    private Long parentId;
    private String username;
    private Long sort;
    private Boolean autoSudo;
    private String name;
    private String ip;
    private Long port;
    private String password;
    private String key;
    private Long proxyId;
}

