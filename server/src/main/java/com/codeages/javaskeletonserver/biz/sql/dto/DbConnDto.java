package com.codeages.javaskeletonserver.biz.sql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbConnDto {
    private Long id;
    private String connName;
    private String username;
    private String password;
    private String host;
    private String port;
    private Boolean savePassword;
}

