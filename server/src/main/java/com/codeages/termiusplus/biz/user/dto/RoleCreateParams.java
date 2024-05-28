package com.codeages.termiusplus.biz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleCreateParams {
    private String name;
    private String serverPermission;
}

