package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.server.dto.ServerCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ServerUpdateParams;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.server.ws.ssh.SshHandler;
import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import com.codeages.javaskeletonserver.biz.user.service.RoleService;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.biz.util.QueryUtils;
import com.codeages.javaskeletonserver.common.IdPayload;
import com.codeages.javaskeletonserver.common.OkResponse;
import com.codeages.javaskeletonserver.security.SecurityContext;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.codeages.javaskeletonserver.biz.server.context.ServerContext.SSH_POOL;

@RestController
@RequestMapping("/api-admin/server")
public class ServerController {

    private final ServerService serverService;

    private final SecurityContext securityContext;

    private final RoleService roleService;

    private final UserService userService;

    public ServerController(ServerService serverService, SecurityContext securityContext, RoleService roleService,
                            UserService userService) {
        this.serverService = serverService;
        this.securityContext = securityContext;
        this.roleService = roleService;
        this.userService = userService;
    }


    @GetMapping("/list")
    public List<Tree<Long>> findAll() {

        List<Long> roleIds = securityContext.getUser().getRoleIds();
        List<RoleDto> service = roleService.findByIds(roleIds);
        List<Long> serverIds = new ArrayList<>();

        service.stream()
               .map(roleDto -> JSONUtil.parseArray(roleDto.getServerPermission()))
               .forEach(jsonArray -> jsonArray.forEach(o -> serverIds.add(Long.valueOf(o.toString()))));

        List<Tree<Long>> treeList = serverService.findAll(serverIds);

        //拿到当前在线的连接
        List<SshHandler.HandlerItem> handlerItemArrayList = new ArrayList<>(SSH_POOL.values());
        QueryUtils.batchQueryOneToOne(
                handlerItemArrayList,
                SshHandler.HandlerItem::getUserId,
                userService::findAllByIdIn,
                UserDto::getId,
                SshHandler.HandlerItem::setUserDto
        );

        Map<Long, List<SshHandler.HandlerItem>> serverIdMap = handlerItemArrayList.stream()
                                                                                  .collect(Collectors.groupingBy(
                                                                                          SshHandler.HandlerItem::getServerId));

        treeList.forEach(tree -> tree.walk(node -> {
            List<SshHandler.HandlerItem> handlerItems = serverIdMap.get(node.getId());
            if (handlerItems != null) {
                List<Map<String, Object>> list = handlerItems.stream()
                                                             .map(h -> Map.of(
                                                                     "masterSessionId",
                                                                     h.getMasterSessionId(),
                                                                     "user",
                                                                     h.getUserDto()
                                                             ))
                                                             .collect(Collectors.toList());
                node.putExtra("onlyConnect", list);
            }
        }));


        return treeList;
    }

    /**
     * 给有onlyConnect或者
     * @return
     */

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

    @GetMapping("/{serverId}/history")
    public List<String> getHistory(@PathVariable Long serverId) {
        return serverService.getHistory(serverId);
    }

}


