package com.codeages.termiusplus.api.admin;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandSearchParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import com.codeages.termiusplus.biz.snippet.service.CommandService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.common.PagerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping("/api-admin/command")
public class CommandController {

    private final CommandService commandService;

    private final ServerService serverService;

    public CommandController(CommandService commandService, ServerService serverService) {
        this.commandService = commandService;
        this.serverService = serverService;
    }

    @GetMapping("/search")
    public PagerResponse<CommandDto> search(CommandSearchParams searchParams,
                                            @PageableDefault(size = 20, sort = {"createdAt"}, direction = Sort.Direction.DESC) Pageable pager) {
        Page<CommandDto> page = commandService.search(searchParams, pager);

        return new PagerResponse<>(
                page,
                pager
        );
    }

    @GetMapping("/list")
    public List<CommandDto> list(CommandSearchParams searchParams) {
        List<CommandDto> content = commandService.search(searchParams, Pageable.unpaged()).getContent();
        List<Long> ids = content
                             .stream()
                             .map(CommandDto::getServerIds)
                             .filter(StrUtil::isNotEmpty)
                             .map(s -> Arrays.stream(s.split(",")))
                             .reduce(Stream::concat).orElse(Stream.empty()).map(Long::parseLong).collect(Collectors.toList());

        Map<Long, ServerDto> idServerDtoMap = serverService.findByIdIn(ids)
                                                           .stream()
                                                           .collect(Collectors.toMap(s -> s.getId(), Function.identity()));

        content.forEach(commandDto -> {
            if (StrUtil.isNotEmpty(commandDto.getServerIds())) {
                List<ServerDto> serverDtos = Arrays.stream(commandDto.getServerIds().split(","))
                                                   .map(Long::parseLong)
                                                   .map(idServerDtoMap::get)
                                                   .collect(Collectors.toList());
                commandDto.setServerDtos(serverDtos);
            }
        });

        return content;
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody CommandCreateParams createParams) {
        commandService.create(createParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody CommandUpdateParams updateParams) {
        commandService.update(updateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        commandService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

}


