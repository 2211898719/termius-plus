package com.codeages.termiusplus.biz.sql.service.impl;

import cn.hutool.db.Db;
import cn.hutool.db.Entity;
import cn.hutool.db.Page;
import cn.hutool.db.PageResult;
import com.alibaba.druid.pool.DruidDataSource;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.PortForWardingService;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.sql.dto.*;
import com.codeages.termiusplus.biz.sql.service.DbConnService;
import com.codeages.termiusplus.common.PagerResponse;
import com.codeages.termiusplus.exception.AppException;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
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

    private static final Map<Long, DataSource> serverDsContainer = new ConcurrentHashMap<>();

    private final DbConnService dbConnService;

    private final ServerService serverService;

    private final PortForWardingService portForWardingService;

    @Value("${current.ip}")
    private String currentIp;

    public DbConnectOperationService(DbConnService dbConnService, ServerService serverService,
                                     PortForWardingService portForWardingService) {
        this.dbConnService = dbConnService;
        this.serverService = serverService;
        this.portForWardingService = portForWardingService;
    }

    /**
     * 获取数据源对象的方法 （所有的接口操作都需要经过这个步骤）
     *
     * @param dbId 连接参数实体ID
     * @return 参数对应的数据源对象
     */
    public synchronized DataSource getDsByIdAndType(Long dbId, Boolean type) {
        if (Boolean.TRUE.equals(type)) {
            DataSource dataSource = serverDsContainer.get(dbId);
            if (Objects.nonNull(dataSource)) {
                return dataSource;
            }

            ServerDto serverDto = serverService.findById(dbId);
            String[] split = serverDto.getDbPort().split(",");
            Integer port = Integer.valueOf(split[0]);
            Integer localPort = portForWardingService.startPortForwarding("数据库端口转发", dbId, port);
            DruidDataSource druidDs = getDruidDataSource(currentIp, localPort.toString(), "root", "root");
            serverDsContainer.put(dbId, druidDs);
            return druidDs;
        }

        DataSource dataSource = dsContainer.get(dbId);
        if (Objects.nonNull(dataSource)) {
            return dataSource;
        }

        DbConnDto dbConnDto = dbConnService.findById(dbId)
                                           .orElseThrow(() -> new AppException(
                                                   ErrorCode.INVALID_ARGUMENT,
                                                   "数据库连接不存在"
                                           ));
        DruidDataSource druidDs = getDruidDataSource(
                dbConnDto.getHost(),
                dbConnDto.getPort(),
                dbConnDto.getUsername(),
                dbConnDto.getPassword()
        );
        dsContainer.put(dbId, druidDs);
        return druidDs;
    }

    /**
     * 获取数据源所有表名
     */
    @SneakyThrows
    public DbTablesDto getTableNames(DbTableQueryDTO dto) {
        Db db = Db.use(getDsByIdAndType(dto.getDbId(), dto.getType()));

        Entity condition = Entity.create();
        condition.setTableName("`information_schema`.`TABLES`");
        condition.set("`TABLE_TYPE`", "BASE TABLE");
        condition.set("`TABLE_SCHEMA`", dto.getSchemaName());
        if (StringUtils.isNotBlank(dto.getTableName()))
            condition.put("`TABLE_NAME`", "LIKE '%" + dto.getTableName() + "%'");
        if (StringUtils.isNotBlank(dto.getTableComment()))
            condition.put("`TABLE_COMMENT`", "LIKE '%" + dto.getTableComment() + "%'");

        List<Entity> entities = db.find(condition);

        //获取所有的表的列信息
        condition = Entity.create();
        condition.setTableName("`information_schema`.`COLUMNS`");
        condition.set("`TABLE_SCHEMA`", dto.getSchemaName());
        List<Entity> columns = db.find(condition);
        return new DbTablesDto(entities, columns);
    }

    /**
     * 获取数据源所有数据库名
     */
    @SneakyThrows
    public List<DatabaseDto> getDatabaseNames(Long dbId, Boolean type) {
        Db db = Db.use(getDsByIdAndType(dbId, type));

        List<Entity> entities = db.findAll(Entity.create().setTableName("`information_schema`.`SCHEMATA`"));
        return entities.stream()
                       .map(entity -> new DatabaseDto(entity.getStr("SCHEMA_NAME")))
                       .collect(Collectors.toList());
    }


    private static DruidDataSource getDruidDataSource(String host, String port, String username, String password) {
        DruidDataSource druidDs = new DruidDataSource();
        druidDs.setUrl("jdbc:mysql://" + host + ":" + port + "/sys");
        druidDs.setUsername(username);
        druidDs.setPassword(password);
        druidDs.addConnectionProperty("useInformationSchema", "true");
        druidDs.addConnectionProperty("characterEncoding", "utf-8");
        druidDs.addConnectionProperty("useSSL", "false");
        druidDs.addConnectionProperty("serverTimezone", "UTC");
        druidDs.addConnectionProperty("useAffectedRows", "true");
        return druidDs;
    }

    @SneakyThrows
    public PagerResponse<Entity> selectTableData(SelectTableDTO searchParams, Pageable pager) {
        Db db = Db.use(getDsByIdAndType(searchParams.getDbId(), searchParams.getType()));
        Entity condition = Entity.create();
        condition.setTableName(searchParams.getSchemaName() + "." + searchParams.getTableName());
        PageResult<Entity> pageResult = db.page(condition, new Page(pager.getPageNumber(), pager.getPageSize()));
        return new PagerResponse<>(new ArrayList<>(pageResult), (long) pageResult.getTotal(), pager);
    }

    @SneakyThrows
    public List<Entity> getTableColumns(SelectTableDTO searchParams) {
        Db db = Db.use(getDsByIdAndType(searchParams.getDbId(), searchParams.getType()));
        Entity condition = Entity.create();
        condition.setTableName("`information_schema`.`COLUMNS`");
        condition.set("`TABLE_SCHEMA`", searchParams.getSchemaName());
        condition.set("`TABLE_NAME`", searchParams.getTableName());
        return db.find(condition);
    }

    @SneakyThrows
    public String getTableKeyColumns(SelectTableDTO searchParams){
        Db db = Db.use(getDsByIdAndType(searchParams.getDbId(), searchParams.getType()));
        Entity condition = Entity.create();
        condition.setTableName("`information_schema`.`COLUMNS`");
        condition.set("`TABLE_SCHEMA`", searchParams.getSchemaName());
        condition.set("`TABLE_NAME`", searchParams.getTableName());
        condition.set("`COLUMN_KEY`", "PRI");
        List<Entity> entities = db.find(condition);
        return entities.stream()
                       .map(entity -> entity.getStr("COLUMN_NAME"))
                       .collect(Collectors.joining(","));
    }

    @SneakyThrows
    public List<Entity> executeSql(Long dbId, Boolean type, String sql) {
        Db db = Db.use(getDsByIdAndType(dbId, type));
        return db.query(sql);
    }

}
