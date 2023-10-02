package com.codeages.javaskeletonserver.biz.server.dto;

import com.codeages.javaskeletonserver.biz.server.enums.ProxyTypeEnum;
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

