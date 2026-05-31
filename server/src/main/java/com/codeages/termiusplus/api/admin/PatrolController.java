package com.codeages.termiusplus.api.admin;

import com.codeages.termiusplus.biz.patrol.agent.PatrolAgentService;
import com.codeages.termiusplus.biz.patrol.dto.*;
import com.codeages.termiusplus.biz.patrol.entity.PatrolConversation;
import com.codeages.termiusplus.biz.patrol.entity.PatrolMessage;
import com.codeages.termiusplus.biz.patrol.repository.PatrolConversationRepository;
import com.codeages.termiusplus.biz.patrol.repository.PatrolMessageRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolEngine;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.biz.server.entity.Server;
import com.codeages.termiusplus.biz.server.repository.ServerRepository;
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
    private final ServerRepository serverRepository;

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

        // 构建包含服务器上下文的消息
        String message = buildMessageWithContext(params);

        // 确保对话记录存在
        conversationRepository.findByConversationId(conversationId)
                .orElseGet(() -> {
                    PatrolConversation conv = new PatrolConversation();
                    conv.setConversationId(conversationId);
                    conv.setTitle(params.getMessage().length() > 10
                            ? params.getMessage().substring(0, 10)
                            : params.getMessage());
                    return conversationRepository.save(conv);
                });

        return patrolAgentService.stream(message, conversationId);
    }

    /**
     * 将用户消息与 @mention 的服务器上下文合并，让模型能准确识别服务器
     */
    private String buildMessageWithContext(AgentChatParams params) {
        List<Long> serverIdList = params.getServerIdList();
        if (serverIdList.isEmpty()) {
            return params.getMessage();
        }

        List<Server> servers = serverRepository.findAllById(serverIdList);
        if (servers.isEmpty()) {
            return params.getMessage();
        }

        StringBuilder sb = new StringBuilder();
        sb.append("## 用户消息\n").append(params.getMessage()).append("\n\n");
        sb.append("## 用户提及的服务器信息\n");
        for (Server s : servers) {
            sb.append(String.format("- [ID:%d] %s (IP:%s, 端口:%d, 系统:%s, 用户:%s)\n",
                    s.getId(), s.getName(), s.getIp(), s.getPort(), s.getOs(), s.getUsername()));
        }
        sb.append("\n注意：当用户消息中提到服务器名称时，请以上方 ID 为准进行工具调用，不要从名称中猜测数字作为 ID。");
        return sb.toString();
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
