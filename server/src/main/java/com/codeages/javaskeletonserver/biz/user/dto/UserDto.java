package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.Data;

@Data
public class UserDto {

    private Long id;

    private String username;

    private Boolean locked;

    private String email;

    private String avatar;

    private Long registerAt;

    private String registerIp;

    private Long loginAt;

    private String loginIp;
}
