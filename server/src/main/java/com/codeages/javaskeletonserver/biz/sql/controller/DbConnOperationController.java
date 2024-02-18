package com.codeages.javaskeletonserver.biz.sql.controller;

import cn.hutool.core.util.StrUtil;
import cn.hutool.db.Entity;
import com.codeages.javaskeletonserver.biz.sql.dto.DatabaseDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbTableQueryDTO;
import com.codeages.javaskeletonserver.biz.sql.dto.DbTablesDto;
import com.codeages.javaskeletonserver.biz.sql.dto.SelectTableDTO;
import com.codeages.javaskeletonserver.biz.sql.service.impl.DbConnectOperationService;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api-admin/dbConnOperation")
public class DbConnOperationController {

    private final DbConnectOperationService dbConnectOperationService;

    public DbConnOperationController(DbConnectOperationService dbConnectOperationService) {
        this.dbConnectOperationService = dbConnectOperationService;
    }

    @GetMapping("/showDatabase")
    public List<DatabaseDto> showDatabase(DbTableQueryDTO searchParams) {
        return dbConnectOperationService.getDatabaseNames(searchParams.getDbId(), true);
    }

    @GetMapping("/showTables")
    public DbTablesDto showTables(DbTableQueryDTO searchParams) {
        return dbConnectOperationService.getTableNames(searchParams);
    }

    @GetMapping("/getTableColumns")
    public List<Entity> getTableColumns(SelectTableDTO searchParams) {
        return dbConnectOperationService.getTableColumns(searchParams);
    }

    @GetMapping("/selectTableData")
    public PagerResponse<Entity> selectTableData(SelectTableDTO searchParams,
                                                 @PageableDefault(size = 20) Pageable pager) {
        return dbConnectOperationService.selectTableData(searchParams, pager);
    }

    @GetMapping("/executeSql")
    public List<Entity> executeSql(DbTableQueryDTO searchParams) {
        return dbConnectOperationService.executeSql(searchParams.getDbId(), true, searchParams.getSql());
    }


}


