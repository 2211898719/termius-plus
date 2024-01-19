package com.codeages.javaskeletonserver.biz.user.dto;

import lombok.Data;
import org.hibernate.validator.constraints.Length;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotEmpty;
import java.util.List;

@Data
public class UserUpdateParams {
    private Long id;

    @NotEmpty
    private String password;

    private List<Long> roleIds;
}
