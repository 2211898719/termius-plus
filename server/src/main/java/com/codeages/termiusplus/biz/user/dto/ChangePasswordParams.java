package com.codeages.termiusplus.biz.user.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordParams {
    private Long userId;
    private String oldPassword;
    private String newPassword;
}
