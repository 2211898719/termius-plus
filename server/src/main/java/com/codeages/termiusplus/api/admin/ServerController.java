package com.codeages.termiusplus.api.admin;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.lang.tree.Tree;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.server.dto.*;
import com.codeages.termiusplus.biz.server.job.ConnectTimeoutJob;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.ws.ssh.SshHandler;
import com.codeages.termiusplus.biz.user.dto.RoleDto;
import com.codeages.termiusplus.biz.user.dto.UserDto;
import com.codeages.termiusplus.biz.user.service.RoleService;
import com.codeages.termiusplus.biz.user.service.UserService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.codeages.termiusplus.security.SecurityContext;
import com.dingtalk.open.app.api.OpenDingTalkStreamClientBuilder;
import com.dingtalk.open.app.api.security.AuthClientCredential;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.codeages.termiusplus.biz.server.context.ServerContext.SSH_POOL;

@RestController
@RequestMapping("/api-admin/server")
@Slf4j
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

        buildOnlyTag(treeList);

        return treeList;
    }

    @PostMapping("/clearConnect")
    public OkResponse clearConnect() {
        log.info("强制清除所有连接");

        ConnectTimeoutJob.clearConnect(true);

        return OkResponse.TRUE;
    }


    /**
     * 递归构建在线标记，如果子节点有一个onlyConnect，那么父节点加上onlyTag: 数量
     *
     * @param treeList
     */
    private int buildOnlyTag(List<Tree<Long>> treeList) {
        int onlyTag = 0;
        for (Tree<Long> node : treeList) {
            int currentTag = node.hasChild() ? buildOnlyTag(node.getChildren()) : ((List) node.getOrDefault(
                    "onlyConnect",
                    Collections.emptyList()
            )).size();
            node.putExtra("onlyTag", currentTag);

            onlyTag = onlyTag + currentTag;
        }

        return onlyTag;
    }

    /**
     * 给有onlyConnect或者
     *
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

    @GetMapping("/{serverId}/mysqlHistory")
    public List<String> getMysqlHistory(@PathVariable Long serverId) {
        return serverService.getMysqlHistory(serverId);
    }

    @GetMapping("/{serverId}/get")
    public ServerDto get(@PathVariable Long serverId) {
        return serverService.findById(serverId);
    }

    @GetMapping("/getAllServerRunInfo")
    public List<ServerRunLogDTO> getAllServerRunInfo() {
        List<ServerDto> allServer = serverService.findAllTestInfoServer();
        List<ServerRunLogDTO> serverRunLogDTOList = serverService.getServerLastRunInfoAfterLimit(DateUtil.offsetDay(DateUtil.date(), -3));

        Map<Long, List<ServerRunLogDTO>> group = serverService.getServerLastRunInfoAfter(DateUtil.offsetDay(DateUtil.date(), -365)).stream().collect(Collectors.groupingBy(ServerRunLogDTO::getServerId));

        Map<Long, ServerRunLogDTO> infoMap = serverRunLogDTOList.stream().collect(Collectors.toMap(ServerRunLogDTO::getServerId, Function.identity()));

        return allServer.stream().map(server -> {
            ServerRunLogDTO serverRunLogDTO = infoMap.get(server.getId());
            if (serverRunLogDTO == null) {
                return new ServerRunLogDTO(server, false, server.getName(), server.getIp(), server.getPort());
            }

            serverRunLogDTO.setServer(server);
            serverRunLogDTO.setInfoStatus(true);
            serverRunLogDTO.setServerName(server.getName());
            serverRunLogDTO.setServerIp(server.getIp());
            serverRunLogDTO.setServerPort(server.getPort());
            serverRunLogDTO.setDetail(group.get(server.getId()));

            return serverRunLogDTO;
        }).collect(Collectors.toList());
    }

    @GetMapping("/syncAllServerRunInfo")
    public List<ServerRunLogDTO> syncAllServerRunInfo() {
        return serverService.syncAllServerRunInfo();
    }

}


