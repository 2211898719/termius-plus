package com.codeages.termiusplus.biz.user.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserCreateParams {
    @NotEmpty
    @Length(min = 2, max = 64)
    private String username;

    @NotEmpty
    @Length(min = 5, max = 64)
    private String password;

    @Email
    private String email;

    private List<Long> roleIds;
}
