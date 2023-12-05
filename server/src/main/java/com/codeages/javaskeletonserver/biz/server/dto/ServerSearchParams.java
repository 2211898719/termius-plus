package com.codeages.javaskeletonserver.biz.server.dto;

import com.codeages.javaskeletonserver.biz.server.enums.OSEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerSearchParams {
    private String name;
    private String ip;
    private String username;
    private Boolean isGroup;
    private String firstCommand;
    private Long sort;
    private Boolean autoSudo;
    private OSEnum os;
    private Long parentId;
    private Long port;
    private String password;
    private String key;
    private Long proxyId;
}

