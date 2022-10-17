package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class UserWithRolesDto extends UserDto {

    private List<String> roles = new ArrayList<>();
}
