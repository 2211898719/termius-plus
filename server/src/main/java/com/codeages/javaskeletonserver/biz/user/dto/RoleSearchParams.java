package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RoleSearchParams {
    private String name;
    private String serverPermission;
}

