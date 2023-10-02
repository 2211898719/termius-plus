package com.codeages.javaskeletonserver.biz.server.dto;

import com.codeages.javaskeletonserver.biz.server.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class GroupSearchParams {
    private String name;
    private Long parentId;
    private Long proxyId;
    private ProxyTypeEnum type;
    private String username;
}

