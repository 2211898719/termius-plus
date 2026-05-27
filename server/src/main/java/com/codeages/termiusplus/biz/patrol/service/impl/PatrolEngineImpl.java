package com.codeages.termiusplus.biz.patrol.service.impl;

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
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.github.jaemon.dinger.core.entity.enums.MessageSubType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolEngineImpl implements PatrolEngine {

    private final PatrolScriptRepository scriptRepository;
    private final PatrolTaskRepository taskRepository;
    private final PatrolTaskMapper taskMapper;
    private final ServerService serverService;
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
    public List<PatrolTaskDto> executeAll() {
        List<PatrolScript> scripts = scriptRepository.findAllByEnabledTrue();
        List<ServerDto> servers = serverService.findAllTestInfoServer();
        List<PatrolTaskDto> results = new ArrayList<>();

        for (PatrolScript script : scripts) {
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
        List<ServerDto> servers = serverService.findAllTestInfoServer();
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

    private void sendAlert(String scriptName, String serverName, String status, String message) {
        String title = "AI 巡查告警";
        String content = String.format("\n## %s\n- **脚本**: %s\n- **服务器**: %s\n- **状态**: %s\n- **详情**: %s",
                title, scriptName, serverName, status, message);
        messageService.send(MessageSubType.MARKDOWN, title, content);
    }
}
