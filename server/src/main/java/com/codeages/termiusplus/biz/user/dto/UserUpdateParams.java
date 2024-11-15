package com.codeages.termiusplus.biz.user.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class UserUpdateParams {
    private Long id;

    @NotEmpty
    private String password;

    private List<Long> roleIds;
}
