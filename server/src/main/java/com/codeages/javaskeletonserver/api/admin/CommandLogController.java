package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogSearchParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogUpdateParams;

import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Pageable;

import javax.annotation.security.RolesAllowed;

@RestController
@RequestMapping("/api-admin/commandLog")
public class CommandLogController {

    private final CommandLogService commandLogService;

    public CommandLogController(CommandLogService commandLogService) {
        this.commandLogService = commandLogService;
    }

    @GetMapping("/search")
    public PagerResponse<CommandLogDto> search(@RequestParam CommandLogSearchParams searchParams,
                                               @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        return new PagerResponse<>(
                commandLogService.search(searchParams, pager),
                pager
        );
    }

    @PostMapping("/create")
    
    public OkResponse create(@RequestBody CommandLogCreateParams createParams) {
        commandLogService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    
    public OkResponse update(@RequestBody CommandLogUpdateParams updateParams) {
        commandLogService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        commandLogService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


