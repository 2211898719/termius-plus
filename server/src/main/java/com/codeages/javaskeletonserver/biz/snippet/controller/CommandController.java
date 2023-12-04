package com.codeages.javaskeletonserver.biz.snippet.controller;

import com.codeages.javaskeletonserver.biz.snippet.service.CommandService;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandDto;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandSearchParams;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandCreateParams;
import com.codeages.javaskeletonserver.biz.snippet.dto.CommandUpdateParams;


import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api-admin/command")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping("/search")
    public PagerResponse<CommandDto> search(@RequestParam CommandSearchParams searchParams,
                                            @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                commandService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse create(@RequestBody CommandCreateParams createParams) {
        commandService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse update(@RequestBody CommandUpdateParams updateParams) {
        commandService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    @RolesAllowed("ROLE_ADMIN")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        commandService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


