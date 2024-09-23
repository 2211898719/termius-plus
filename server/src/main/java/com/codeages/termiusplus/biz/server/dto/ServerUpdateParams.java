package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.server.enums.OSEnum;
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
    private OSEnum os;
    private Boolean infoTest;
    private Boolean autoSudo;
    private Long port;
    private String password;
    private String key;
    private Boolean isDb;
    private Boolean keepAlive;
    private String dbPort;
    private String remark;
    private Long proxyId;
}

