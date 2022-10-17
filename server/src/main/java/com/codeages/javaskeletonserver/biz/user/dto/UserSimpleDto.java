package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.Data;

@Data
public class UserSimpleDto {
    private Long id;

    private String username;

    private String avatar;
}
