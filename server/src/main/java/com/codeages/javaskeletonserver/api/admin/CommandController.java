package com.codeages.javaskeletonserver.api.admin;

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
import java.util.List;

@RestController
@RequestMapping("/api-admin/command")
public class CommandController {

    private final CommandService commandService;

    public CommandController(CommandService commandService) {
        this.commandService = commandService;
    }

    @GetMapping("/search")
    public PagerResponse<CommandDto> search(CommandSearchParams searchParams,
                                            @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                commandService.search(searchParams, pager),
                pager
        );
    }

    @GetMapping("/list")
    public List<CommandDto> list(CommandSearchParams searchParams) {
        return commandService.search(searchParams, Pageable.unpaged()).getContent();
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


