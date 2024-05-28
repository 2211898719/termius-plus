package com.codeages.termiusplus.biz.user.dto;

import lombok.Data;

import java.util.List;

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

    private List<Long> roleIds;
}
