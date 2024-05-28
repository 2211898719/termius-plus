package com.codeages.termiusplus.biz.sql.controller;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.sql.dto.DbConnCreateParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnDto;
import com.codeages.termiusplus.biz.sql.dto.DbConnSearchParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnUpdateParams;
import com.codeages.termiusplus.biz.sql.service.DbConnService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/dbConn")
public class DbConnController {

    private final DbConnService dbConnService;

    private final ServerService serverService;

    public DbConnController(DbConnService dbConnService, ServerService serverService) {
        this.dbConnService = dbConnService;
        this.serverService = serverService;
    }

    @GetMapping("/list")
    public List<DbConnDto> list(DbConnSearchParams searchParams) {
        return dbConnService.search(searchParams, Pageable.unpaged()).getContent();
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody DbConnCreateParams createParams) {
        dbConnService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody DbConnUpdateParams updateParams) {
        dbConnService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        dbConnService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

    @GetMapping("/get/{id}")
    public DbConnDto get(@PathVariable Long id) {
        return dbConnService.findById(id).orElse(null);
    }

    @GetMapping("/findAllDbServer")
    public List<Tree<Long>> findAllDbServer() {
        return serverService.findAllDb();
    }

}


