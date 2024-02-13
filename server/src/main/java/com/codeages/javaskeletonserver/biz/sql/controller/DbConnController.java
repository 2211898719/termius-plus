package com.codeages.javaskeletonserver.biz.sql.controller;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnCreateParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnSearchParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnUpdateParams;
import com.codeages.javaskeletonserver.biz.sql.service.DbConnService;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import javax.annotation.security.RolesAllowed;
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


