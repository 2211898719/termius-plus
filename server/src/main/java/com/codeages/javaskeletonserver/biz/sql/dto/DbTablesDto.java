package com.codeages.javaskeletonserver.biz.sql.dto;

import cn.hutool.db.Entity;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DbTablesDto {
    private List<Entity> tables;
    private List<Entity> columns;
}
