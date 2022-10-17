package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.Data;

@Data
public class LoginParams {
    private String username;

    private String password;
}
