package com.codeages.termiusplus.biz.patrol.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.message.MessageService;
import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import com.codeages.termiusplus.biz.patrol.entity.PatrolTask;
import com.codeages.termiusplus.biz.patrol.mapper.PatrolTaskMapper;
import com.codeages.termiusplus.biz.patrol.repository.PatrolScriptRepository;
import com.codeages.termiusplus.biz.patrol.repository.PatrolTaskRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.entity.Server;
import com.codeages.termiusplus.biz.server.repository.ServerRepository;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolEngineImpl implements PatrolEngine {

    private final PatrolScriptRepository scriptRepository;
    private final PatrolTaskRepository taskRepository;
    private final PatrolTaskMapper taskMapper;
    private final ServerService serverService;
    private final ServerRepository serverRepository;
    private final MessageService messageService;

    @Override
    public PatrolTaskDto executeScript(Long scriptId, Long serverId) {
        PatrolScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("脚本不存在"));

        PatrolTask task = new PatrolTask();
        task.setScriptId(scriptId);
        task.setServerId(serverId);
        task.setExecutedAt(new Date());

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String output = client.executeCommand(script.getScriptContent());
            task.setOutput(output);

            Map<String, Object> result = JSONUtil.parseObj(output);
            String status = (String) result.getOrDefault("status", "error");
            task.setStatus(status);

            if ("warning".equals(status) || "error".equals(status)) {
                String message = (String) result.getOrDefault("message", "未知异常");
                ServerDto server = serverService.findById(serverId);
                sendAlert(script.getName(), server.getName(), status, message);
                task.setAlertSent(true);
            } else {
                task.setAlertSent(false);
            }
        } catch (Exception e) {
            log.error("执行巡检脚本失败: scriptId={}, serverId={}", scriptId, serverId, e);
            task.setStatus("error");
            task.setOutput("{\"status\":\"error\",\"message\":\"执行失败: " + e.getMessage() + "\"}");
            try {
                ServerDto server = serverService.findById(serverId);
                sendAlert(script.getName(), server.getName(), "error", "脚本执行失败: " + e.getMessage());
            } catch (Exception ex) {
                log.error("发送告警失败", ex);
            }
            task.setAlertSent(true);
        }

        return taskMapper.toDto(taskRepository.save(task));
    }

    @Override
    public PatrolTaskDto executeDraft(String scriptContent, String scriptName, Long serverId) {
        // 测试场景:用户自己触发自己看结果,不触发钉钉告警
        // (dinger-spring-boot-starter 2.1.0 与 spring-web 7 存在 IncompatibleClassChangeError,告警调用会 500)
        PatrolTask task = new PatrolTask();
        task.setScriptId(null);
        task.setServerId(serverId);
        task.setExecutedAt(new Date());

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String output = client.executeCommand(scriptContent);
            task.setOutput(output);

            Map<String, Object> result = JSONUtil.parseObj(output);
            String status = (String) result.getOrDefault("status", "error");
            task.setStatus(status);
        } catch (Exception e) {
            log.error("测试执行巡检脚本失败: serverId={}", serverId, e);
            task.setStatus("error");
            task.setOutput("{\"status\":\"error\",\"message\":\"执行失败: " + e.getMessage() + "\"}");
        }
        // 测试不落库,只把内存中的 task 转 DTO 返回
        return taskMapper.toDto(task);
    }

    @Override
    public List<PatrolTaskDto> executeAll() {
        List<PatrolScript> scripts = scriptRepository.findAllByEnabledTrue();
        List<PatrolTaskDto> results = new ArrayList<>();

        for (PatrolScript script : scripts) {
            List<ServerDto> servers = resolveTargetServers(script);
            for (ServerDto server : servers) {
                try {
                    results.add(executeScript(script.getId(), server.getId()));
                } catch (Exception e) {
                    log.error("执行巡检失败: script={}, server={}", script.getId(), server.getId(), e);
                }
            }
        }
        return results;
    }

    @Override
    public List<PatrolTaskDto> executeScriptOnAllServers(Long scriptId) {
        PatrolScript script = scriptRepository.findById(scriptId)
                .orElseThrow(() -> new RuntimeException("脚本不存在"));
        List<ServerDto> servers = resolveTargetServers(script);
        List<PatrolTaskDto> results = new ArrayList<>();
        for (ServerDto server : servers) {
            try {
                results.add(executeScript(scriptId, server.getId()));
            } catch (Exception e) {
                log.error("执行巡检失败: scriptId={}, serverId={}", scriptId, server.getId(), e);
            }
        }
        return results;
    }

    /**
     * 解析脚本的服务器范围：
     * - serverIds 和 groupIds 都为空 → 全部服务器（保持旧行为）
     * - 否则返回 指定服务器 ∪ 指定分组（含子分组）下所有服务器 的并集去重
     */
    private List<ServerDto> resolveTargetServers(PatrolScript script) {
        List<Long> serverIds = parseIdList(script.getServerIds());
        List<Long> groupIds = parseIdList(script.getGroupIds());

        if (serverIds.isEmpty() && groupIds.isEmpty()) {
            return serverService.findAllTestInfoServer();
        }

        Set<Long> targetServerIds = new HashSet<>(serverIds);

        // 递归展开分组 → 子分组 → 末端服务器
        Set<Long> visitedGroups = new HashSet<>();
        List<Long> queue = new ArrayList<>(groupIds);
        while (!queue.isEmpty()) {
            List<Long> current = new ArrayList<>(queue);
            queue.clear();
            for (Long gid : current) {
                if (gid == null || !visitedGroups.add(gid)) continue;
                List<Server> children = serverRepository.findAllByParentIdIn(Collections.singleton(gid));
                for (Server child : children) {
                    if (Boolean.TRUE.equals(child.getIsGroup())) {
                        queue.add(child.getId());
                    } else {
                        targetServerIds.add(child.getId());
                    }
                }
            }
        }

        if (targetServerIds.isEmpty()) {
            return Collections.emptyList();
        }
        return serverService.findByIdIn(new ArrayList<>(targetServerIds))
                .stream()
                .filter(s -> !Boolean.TRUE.equals(s.getIsGroup()))
                .collect(Collectors.toList());
    }

    private List<Long> parseIdList(String json) {
        if (StrUtil.isBlank(json)) {
            return Collections.emptyList();
        }
        try {
            return JSONUtil.toList(json, Long.class);
        } catch (Exception e) {
            log.warn("解析脚本服务器范围 ID 列表失败: {}", json, e);
            return Collections.emptyList();
        }
    }

    private void sendAlert(String scriptName, String serverName, String status, String message) {
        String title = "AI 巡查告警";
        String content = String.format("\n## %s\n- **脚本**: %s\n- **服务器**: %s\n- **状态**: %s\n- **详情**: %s",
                title, scriptName, serverName, status, message);
        messageService.send(MessageSubType.MARKDOWN, title, content);
    }
}
