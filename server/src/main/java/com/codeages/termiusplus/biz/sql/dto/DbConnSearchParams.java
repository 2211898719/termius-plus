package com.codeages.termiusplus.biz.sql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbConnSearchParams {
    private String connName;
    private String username;
    private String password;
    private String host;
    private String port;
    private Boolean savePassword;
}

