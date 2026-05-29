package com.codeages.termiusplus.biz.user.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserWithRolesDto extends UserDto {

    private List<String> roles = new ArrayList<>();
}
