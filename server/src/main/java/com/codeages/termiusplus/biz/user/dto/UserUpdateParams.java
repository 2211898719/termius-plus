package com.codeages.termiusplus.biz.user.dto;

import lombok.Data;

import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserUpdateParams {
    private Long id;

    @NotEmpty
    private String password;

    private List<Long> roleIds;
}
