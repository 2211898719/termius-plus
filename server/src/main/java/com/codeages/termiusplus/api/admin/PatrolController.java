package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.patrol.agent.PatrolAgentService;
import com.codeages.termiusplus.biz.patrol.dto.*;
import com.codeages.termiusplus.biz.patrol.entity.PatrolConversation;
import com.codeages.termiusplus.biz.patrol.entity.PatrolMessage;
import com.codeages.termiusplus.biz.patrol.repository.PatrolConversationRepository;
import com.codeages.termiusplus.biz.patrol.repository.PatrolMessageRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.common.IdPayload;
import com.codeages.termiusplus.common.OkResponse;
import com.cxytiandi.encrypt.springboot.annotation.DecryptIgnore;
import com.cxytiandi.encrypt.springboot.annotation.EncryptIgnore;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.*;

@RestController
@RequestMapping("/api-admin/patrol")
@RequiredArgsConstructor
public class PatrolController {

    private final PatrolScriptService scriptService;
    private final PatrolEngine patrolEngine;
    private final PatrolAgentService patrolAgentService;
    private final PatrolConversationRepository conversationRepository;
    private final PatrolMessageRepository messageRepository;

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

        // 确保对话记录存在
        conversationRepository.findByConversationId(conversationId)
                .orElseGet(() -> {
                    PatrolConversation conv = new PatrolConversation();
                    conv.setConversationId(conversationId);
                    conv.setTitle(params.getMessage().length() > 50
                            ? params.getMessage().substring(0, 50) + "..."
                            : params.getMessage());
                    return conversationRepository.save(conv);
                });

        return patrolAgentService.stream(params.getMessage(), conversationId);
    }

    // === 对话管理 ===

    @GetMapping("/agent/conversations")
    public List<Map<String, Object>> listConversations() {
        return conversationRepository.findAll().stream()
                .sorted(Comparator.comparing(PatrolConversation::getUpdatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(c -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("conversationId", c.getConversationId());
                    map.put("title", c.getTitle());
                    map.put("createdAt", c.getCreatedAt());
                    map.put("updatedAt", c.getUpdatedAt());
                    return map;
                })
                .toList();
    }

    @PostMapping("/agent/conversations")
    public Map<String, String> createConversation() {
        String conversationId = UUID.randomUUID().toString();
        PatrolConversation conv = new PatrolConversation();
        conv.setConversationId(conversationId);
        conv.setTitle("新对话");
        conversationRepository.save(conv);
        return Map.of("conversationId", conversationId);
    }

    @Transactional
    @PostMapping("/agent/conversations/delete")
    public OkResponse deleteConversation(@RequestBody Map<String, String> body) {
        String conversationId = body.get("conversationId");
        if (StringUtils.hasText(conversationId)) {
            messageRepository.deleteByConversationId(conversationId);
            conversationRepository.deleteByConversationId(conversationId);
        }
        return OkResponse.TRUE;
    }

    @GetMapping("/agent/messages")
    public List<Map<String, Object>> listMessages(@RequestParam String conversationId) {
        return messageRepository.findByConversationIdOrderBySortOrderAsc(conversationId).stream()
                .map(m -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", m.getId());
                    map.put("role", m.getRole());
                    map.put("content", m.getContent());
                    map.put("timeline", m.getTimeline());
                    map.put("sortOrder", m.getSortOrder());
                    map.put("createdAt", m.getCreatedAt());
                    return map;
                })
                .toList();
    }

    @PostMapping("/agent/messages")
    public OkResponse saveMessage(@RequestBody Map<String, Object> body) {
        String conversationId = (String) body.get("conversationId");
        String role = (String) body.get("role");
        String content = (String) body.get("content");
        String timeline = (String) body.get("timeline");

        if (!StringUtils.hasText(conversationId) || !StringUtils.hasText(role)) {
            return OkResponse.TRUE;
        }

        int count = messageRepository.countByConversationId(conversationId);

        PatrolMessage msg = new PatrolMessage();
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setTimeline(timeline);
        msg.setSortOrder(count + 1);
        messageRepository.save(msg);

        return OkResponse.TRUE;
    }
}
