package com.codeages.javaskeletonserver.biz.sql.service.impl;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import com.alibaba.druid.pool.DruidDataSource;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.sql.dto.DatabaseDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbTableQueryDTO;
import com.codeages.javaskeletonserver.biz.sql.dto.SelectTableDTO;
import com.codeages.javaskeletonserver.biz.sql.service.DbConnService;
import com.codeages.javaskeletonserver.common.PagerResponse;
import com.codeages.javaskeletonserver.exception.AppException;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class DbConnectOperationService {
    private static final Map<Long, DataSource> dsContainer = new ConcurrentHashMap<>();

    private final DbConnService dbConnService;

    private final ServerService serverService;

    public DbConnectOperationService(DbConnService dbConnService, ServerService serverService) {
        this.dbConnService = dbConnService;
        this.serverService = serverService;
    }

    /**
     * 获取数据源对象的方法 （所有的接口操作都需要经过这个步骤）
     *
     * @param dbId 连接参数实体ID
     * @return 参数对应的数据源对象
     */
    public synchronized DataSource getDsById(Long dbId) {
        DataSource dataSource = dsContainer.get(dbId);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }

        DbConnDto dbConnDto = dbConnService.findById(dbId)
                                           .orElseThrow(() -> new AppException(
                                                   ErrorCode.INVALID_ARGUMENT,
                                                   "数据库连接不存在"
                                           ));
        DruidDataSource druidDs = getDruidDataSource(dbConnDto);
        dsContainer.put(dbId, druidDs);
        return druidDs;
    }

    /**
     * 获取数据源所有表名
     */
    @SneakyThrows
    public List<Entity> getTableNames(DbTableQueryDTO dto) {
        Db db = Db.use(getDsById(dto.getDbId()));

        Entity condition = Entity.create();
        condition.setTableName("`information_schema`.`TABLES`");
        condition.set("`TABLE_TYPE`", "BASE TABLE");
        condition.set("`TABLE_SCHEMA`", dto.getSchemaName());
        if (StringUtils.isNotBlank(dto.getTableName()))
            condition.put("`TABLE_NAME`", "LIKE '%" + dto.getTableName() + "%'");
        if (StringUtils.isNotBlank(dto.getTableComment()))
            condition.put("`TABLE_COMMENT`", "LIKE '%" + dto.getTableComment() + "%'");

        return db.find(condition);
    }

    /**
     * 获取数据源所有数据库名
     */
    @SneakyThrows
    public List<DatabaseDto> getDatabaseNames(Long dbId) {
        Db db = Db.use(getDsById(dbId));

        List<Entity> entities = db.findAll(Entity.create().setTableName("`information_schema`.`SCHEMATA`"));
        return entities.stream()
                       .map(entity -> new DatabaseDto(entity.getStr("SCHEMA_NAME")))
                       .collect(Collectors.toList());
    }


    private static DruidDataSource getDruidDataSource(DbConnDto dbConnDto) {
        DruidDataSource druidDs = new DruidDataSource();
        druidDs.setUrl("jdbc:mysql://" + dbConnDto.getHost() + ":" + dbConnDto.getPort() + "/sys");
        druidDs.setUsername(dbConnDto.getUsername());
        druidDs.setPassword(dbConnDto.getPassword());
        druidDs.addConnectionProperty("useInformationSchema", "true");
        druidDs.addConnectionProperty("characterEncoding", "utf-8");
        druidDs.addConnectionProperty("useSSL", "false");
        druidDs.addConnectionProperty("serverTimezone", "UTC");
        druidDs.addConnectionProperty("useAffectedRows", "true");
        return druidDs;
    }

    @SneakyThrows
    public PagerResponse<Entity> selectTableData(SelectTableDTO searchParams, Pageable pager) {
        Db db = Db.use(getDsById(searchParams.getDbId()));
        Entity condition = Entity.create();
        condition.setTableName(searchParams.getSchemaName() + "." + searchParams.getTableName());
        PageResult<Entity> pageResult = db.page(condition, new Page(pager.getPageNumber(), pager.getPageSize()));
        return new PagerResponse<>(new ArrayList<>(pageResult), (long) pageResult.getTotal(), pager);
    }

    @SneakyThrows
    public List<String> getTableColumns(SelectTableDTO searchParams) {
        Db db = Db.use(getDsById(searchParams.getDbId()));
        Entity condition = Entity.create();
        condition.setTableName("`information_schema`.`COLUMNS`");
        condition.set("`TABLE_SCHEMA`", searchParams.getSchemaName());
        condition.set("`TABLE_NAME`", searchParams.getTableName());
        List<Entity> entities = db.find(condition);
        return entities.stream()
                       .map(entity -> entity.getStr("COLUMN_NAME"))
                       .collect(Collectors.toList());
    }
}
