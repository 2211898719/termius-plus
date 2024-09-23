package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.server.enums.OSEnum;
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
    private Boolean infoTest;
    private OSEnum os;
    private String password;
    private Boolean isDb;
    private String dbPort;
    private String key;
    private String remark;
    private Long proxyId;
    private ProxyDto proxy;
    private Boolean keepAlive;
}

