package com.codeages.javaskeletonserver.biz.sql.controller;

import com.codeages.javaskeletonserver.biz.sql.service.DbConnService;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnSearchParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnCreateParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnUpdateParams;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api-admin/dbConn")
public class DbConnController {

    private final DbConnService dbConnService;

    public DbConnController(DbConnService dbConnService) {
        this.dbConnService = dbConnService;
    }

    @GetMapping("/search")
    public PagerResponse<DbConnDto> search(DbConnSearchParams searchParams,
                                           @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                dbConnService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse create(@RequestBody DbConnCreateParams createParams) {
        dbConnService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse update(@RequestBody DbConnUpdateParams updateParams) {
        dbConnService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        dbConnService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

    @GetMapping("/get/{id}")
    public DbConnDto get(@PathVariable Long id) {
        return dbConnService.findById(id).orElse(null);
    }

}


