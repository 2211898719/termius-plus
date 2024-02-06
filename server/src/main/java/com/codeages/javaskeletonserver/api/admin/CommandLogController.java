package com.codeages.javaskeletonserver.api.admin;

import com.codeages.javaskeletonserver.biz.log.dto.*;
import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.biz.util.QueryUtils;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.common.PagerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api-admin/commandLog")
public class CommandLogController {

    private final CommandLogService commandLogService;

    private final ServerService serverService;

    private final UserService userService;

    public CommandLogController(CommandLogService commandLogService, ServerService serverService,
                                UserService userService) {
        this.commandLogService = commandLogService;
        this.serverService = serverService;
        this.userService = userService;
    }

    @GetMapping("/search")
    public PagerResponse<CommandLogSimpleDto> search(CommandLogSearchParams searchParams,
                                               @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        Page<CommandLogSimpleDto> page = commandLogService.search(searchParams, pager);
        QueryUtils.batchQueryOneToOne(
                page.getContent(),
                CommandLogSimpleDto::getServerId,
                serverService::findByIdIn,
                ServerDto::getId,
                (log, server) -> log.setServerName(server.getName() + "(" + server.getIp() + ":" + server.getPort() + ")")
        );
        QueryUtils.batchQueryOneToOne(
                page.getContent(),
                CommandLogSimpleDto::getUserId,
                userService::findAllByIdIn,
                UserDto::getId,
                (log, user) -> log.setUserName(user.getUsername())
        );
        return new PagerResponse<>(
                page,
                pager
        );
    }

    @GetMapping("/list")
    public List<CommandLogSimpleDto> list(CommandLogSearchParams searchParams) {
        return commandLogService.search(searchParams, Pageable.unpaged()).getContent();
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

    @GetMapping("/get/{id}")
    public CommandLogDto get(@PathVariable Long id) {
        return commandLogService.get(id);
    }

}


