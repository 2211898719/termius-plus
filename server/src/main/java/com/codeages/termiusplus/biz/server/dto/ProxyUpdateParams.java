package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.server.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyUpdateParams {
    /**
     * 名称
     */
    private String name;
    private Long id;
    private String ip;
    private Long port;
    private ProxyTypeEnum type;
    private String username;
    private String password;
}

