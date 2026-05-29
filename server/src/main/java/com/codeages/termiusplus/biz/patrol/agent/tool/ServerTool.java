package com.codeages.termiusplus.biz.patrol.agent.tool;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.server.entity.Server;
import com.codeages.termiusplus.biz.server.repository.ServerRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class ServerTool {

    private final ServerRepository serverRepository;

    @Tool(description = "获取服务器树列表，返回所有服务器的分组结构。包含服务器ID、名称、IP、端口、操作系统、用户名等信息，不包含密码等敏感信息。")
    public String getServerTree() {
        try {
            List<Server> servers = serverRepository.findAll();
            List<Map<String, Object>> safeServers = servers.stream()
                    .map(this::toSafeMap)
                    .toList();

            // Build tree structure
            Map<Long, List<Map<String, Object>>> childrenMap = new HashMap<>();
            List<Map<String, Object>> roots = new ArrayList<>();

            for (Map<String, Object> server : safeServers) {
                Long parentId = server.get("parentId") != null ? ((Number) server.get("parentId")).longValue() : null;
                if (parentId == null || parentId == 0) {
                    roots.add(server);
                } else {
                    childrenMap.computeIfAbsent(parentId, k -> new ArrayList<>()).add(server);
                }
            }

            for (Map<String, Object> server : safeServers) {
                Long id = ((Number) server.get("id")).longValue();
                List<Map<String, Object>> children = childrenMap.get(id);
                server.put("children", children != null ? children : Collections.emptyList());
            }

            return JSONUtil.toJsonStr(roots);
        } catch (Exception e) {
            return "获取服务器列表失败: " + e.getMessage();
        }
    }

    private Map<String, Object> toSafeMap(Server server) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", server.getId());
        map.put("name", server.getName());
        map.put("ip", server.getIp());
        map.put("port", server.getPort());
        map.put("os", server.getOs());
        map.put("username", server.getUsername());
        map.put("isGroup", server.getIsGroup());
        map.put("parentId", server.getParentId());
        map.put("remark", server.getRemark());
        map.put("isDb", server.getIsDb());
        return map;
    }
}
