package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.server.dto.ServerCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ServerUpdateParams;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.service.RoleService;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.security.SecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api-admin/server")
public class ServerController {

    private final ServerService serverService;

    private final SecurityContext securityContext;

    private final RoleService roleService;

    public ServerController(ServerService serverService, SecurityContext securityContext, RoleService roleService) {
        this.serverService = serverService;
        this.securityContext = securityContext;
        this.roleService = roleService;
    }


    @GetMapping("/list")
    public List<Tree<Long>> findAll() {

        List<Long> roleIds = securityContext.getUser().getRoleIds();
        List<RoleDto> service = roleService.findByIds(roleIds);
        List<Long> serverIds = new ArrayList<>();
        service.stream()
               .map(roleDto -> JSONUtil.parseArray(roleDto.getServerPermission()))
               .forEach(jsonArray -> jsonArray.forEach(o -> serverIds.add(Long.valueOf(o.toString()))));
        return serverService.findAll(serverIds);
    }

    @GetMapping("/groupList")
    public List<Tree<Long>> groupList() {
        return serverService.groupList();
    }

    @PostMapping("/create")
    public OkResponse create(@RequestBody ServerCreateParams serverCreateParams) {
        serverService.create(serverCreateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/update")
    public OkResponse update(@RequestBody ServerUpdateParams serverUpdateParams) {
        serverService.update(serverUpdateParams);

        return OkResponse.TRUE;
    }

    @PostMapping("/delete")
    public OkResponse delete(@RequestBody IdPayload idPayload) {
        serverService.delete(idPayload.getId());

        return OkResponse.TRUE;
    }

    @PostMapping("/updateSort")
    public OkResponse updateSort(@RequestBody List<TreeSortParams> treeSortParams) {
        serverService.sort(treeSortParams);

        return OkResponse.TRUE;
    }



}


