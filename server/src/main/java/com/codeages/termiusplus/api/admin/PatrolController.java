package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.patrol.agent.PatrolAgentService;
import com.codeages.termiusplus.biz.patrol.dto.*;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.cxytiandi.encrypt.springboot.annotation.DecryptIgnore;
import com.cxytiandi.encrypt.springboot.annotation.EncryptIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api-admin/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolScriptService scriptService;
    private final PatrolEngine patrolEngine;
    private final PatrolAgentService patrolAgentService;

    // === 脚本管理 ===

    @GetMapping("/scripts")
    public List<PatrolScriptDto> listScripts() {
        return scriptService.findAll();
    }

    @GetMapping("/scripts/{id}")
    public PatrolScriptDto getScript(@PathVariable Long id) {
        return scriptService.findById(id);
    }

    @PostMapping("/scripts")
    public PatrolScriptDto createScript(@RequestBody PatrolScriptCreateParams params) {
        return scriptService.create(params);
    }

    @PostMapping("/scripts/update")
    public PatrolScriptDto updateScript(@RequestBody PatrolScriptUpdateParams params) {
        return scriptService.update(params);
    }

    @PostMapping("/scripts/delete")
    public OkResponse deleteScript(@RequestBody IdPayload idPayload) {
        scriptService.delete(idPayload.getId());
        return OkResponse.TRUE;
    }

    @PostMapping("/scripts/generate")
    public PatrolScriptDto generateScript(@RequestBody GenerateScriptParams params) {
        return scriptService.generateScript(params.getDescription());
    }

    // === 执行巡检 ===

    @PostMapping("/execute")
    public PatrolTaskDto execute(@RequestBody PatrolExecuteParams params) {
        return patrolEngine.executeScript(params.getScriptId(), params.getServerId());
    }

    @PostMapping("/execute/script/{scriptId}")
    public List<PatrolTaskDto> executeScriptOnAll(@PathVariable Long scriptId) {
        return patrolEngine.executeScriptOnAllServers(scriptId);
    }

    @PostMapping("/execute/all")
    public List<PatrolTaskDto> executeAll() {
        return patrolEngine.executeAll();
    }

    // === 交互式 Agent ===

    @PostMapping("/agent/chat")
    public AgentChatResponse chat(@RequestBody AgentChatParams params) {
        String conversationId = StringUtils.hasText(params.getConversationId())
                ? params.getConversationId()
                : UUID.randomUUID().toString();

        String reply = patrolAgentService.chat(params.getMessage(), conversationId);
        boolean needsConfirmation = reply != null && reply.contains("[需要用户确认]");
        String pendingCommand = null;
        if (needsConfirmation) {
            int start = reply.indexOf("命令 \"");
            int end = reply.indexOf("\" 不在白名单中");
            if (start >= 0 && end > start) {
                pendingCommand = reply.substring(start + 4, end);
            }
        }
        return new AgentChatResponse(reply, needsConfirmation, pendingCommand, conversationId);
    }

    @GetMapping(value = "/agent/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @EncryptIgnore
    @DecryptIgnore
    public Flux<String> chatStream(AgentChatParams params) {
        String conversationId = StringUtils.hasText(params.getConversationId())
                ? params.getConversationId()
                : UUID.randomUUID().toString();

        return patrolAgentService.stream(params.getMessage(), conversationId);
    }
}
