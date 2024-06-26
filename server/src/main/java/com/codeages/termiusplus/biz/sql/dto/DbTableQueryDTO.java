package com.codeages.termiusplus.biz.sql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbTableQueryDTO {
    private Long dbId;
    private String schemaName;
    private String tableName;
    private String tableComment;
    private String sql;
    private Boolean type;
}
