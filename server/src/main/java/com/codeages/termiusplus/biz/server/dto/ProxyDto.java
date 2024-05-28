package com.codeages.termiusplus.biz.server.dto;

import com.codeages.termiusplus.biz.server.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProxyDto {
    private String name;
    private Long id;
    private String ip;
    private Long port;
    private ProxyTypeEnum type;
    private String password;
    private String username;
}

