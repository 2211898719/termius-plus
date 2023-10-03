package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerUpdateParams {
    private Long id;
    private String name;
    private String ip;
    private Boolean isGroup;
    private Long parentId;
    private String username;
    private String firstCommand;
    private Long sort;
    private Boolean autoSudo;
    private Long port;
    private String password;
    private String key;
    private Long proxyId;
}

