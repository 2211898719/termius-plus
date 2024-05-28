package com.codeages.termiusplus.biz.sql.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SelectTableDTO {
    private Long dbId;
    private String schemaName;
    private String tableName;
    private Boolean type;
}
