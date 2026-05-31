package com.codeages.termiusplus.biz.patrol.agent;

import com.codeages.termiusplus.biz.patrol.agent.tool.*;
import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolAgentService {

    private final CleanupTool cleanupTool;
    private final AgentLlmClientFactory factory;
    private final DiskTool diskTool;
    private final ExecuteCommandTool executeCommandTool;
    private final NginxTool nginxTool;
    private final ServiceTool serviceTool;
    private final ServerTool serverTool;
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT = """
            你是一个 Linux 服务器运维 AI 助手。你可以帮助用户检查和解决服务器问题。
            你可以使用工具来执行命令、检查磁盘、分析 nginx 配置等。
            重要规则：
            1. 只能使用提供的工具，不要自行构造任意命令
            2. 对于危险操作（删除文件、重启服务等），必须先告知用户并获得确认
            3. 先收集信息再分析，给出清晰的问题诊断和解决方案
            4. 如果用户要求执行危险操作，先用 cleanupFiles 等工具列出将要影响的文件，等用户确认后再调用 confirmCleanup 等工具
            5. 输出使用 Markdown 格式，便于前端展示
            """;

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private ChatClient buildChatClient(String conversationId) {
        ChatClient chatClient = factory.createChatClient();
        MessageChatMemoryAdvisor memoryAdvisor = MessageChatMemoryAdvisor.builder(chatMemory)
                                                                          .conversationId(conversationId)
                                                                          .build();
        return chatClient.mutate()
                         .defaultAdvisors(memoryAdvisor)
                         .build();
    }

    public String chat(String userMessage, String conversationId) {
        return buildChatClient(conversationId).prompt(SYSTEM_PROMPT)
                         .tools(cleanupTool, diskTool, executeCommandTool, nginxTool, serviceTool, serverTool)
                         .user(userMessage)
                         .call()
                         .content();
    }

    /**
     * 解析文本中的 <think> 标签，将其转换为单独的 think 事件
     */
    private Flux<String> parseTextWithThink(String text) {
        Pattern thinkPattern = Pattern.compile("<think>(.*?)</think>", Pattern.DOTALL);
        Matcher matcher = thinkPattern.matcher(text);
        ArrayList<String> parts = new ArrayList<>();
        int lastEnd = 0;

        while (matcher.find()) {
            // <think> 之前的普通文本
            if (matcher.start() > lastEnd) {
                String before = text.substring(lastEnd, matcher.start()).trim();
                if (!before.isEmpty()) {
                    parts.add("text:" + before);
                }
            }
            // <think> 内容
            String thinkContent = matcher.group(1).trim();
            if (!thinkContent.isEmpty()) {
                parts.add("think:" + thinkContent);
            }
            lastEnd = matcher.end();
        }

        // </think> 之后的普通文本
        if (lastEnd < text.length()) {
            String after = text.substring(lastEnd).trim();
            if (!after.isEmpty()) {
                parts.add("text:" + after);
            }
        }

        // 如果没有 <think> 标签，直接返回原文
        if (parts.isEmpty() && !text.trim().isEmpty()) {
            parts.add("text:" + text.trim());
        }

        return Flux.fromIterable(parts);
    }

    public Flux<String> stream(String userMessage, String conversationId) {
        // 创建工具调用事件收集器
        var toolEventSink = ToolCallEventCollector.createSink(conversationId);

        // 工具事件流 - 实时发送
        var toolEventFlux = toolEventSink.asFlux()
                .map(event -> {
                    try {
                        return "tool_event:" + objectMapper.writeValueAsString(event);
                    } catch (Exception e) {
                        return "tool_event:{}";
                    }
                });

        // AI 响应流 - 工具调用会通过 sink 发送事件
        var chatFlux = buildChatClient(conversationId).prompt(SYSTEM_PROMPT)
                         .tools(cleanupTool, diskTool, executeCommandTool, nginxTool, serviceTool, serverTool)
                         .user(userMessage)
                         .stream()
                         .chatResponse()
                         .flatMap(response -> {
                             Flux<String> flux = Flux.empty();

                             Generation result = response.getResult();
                             if (result != null) {
                                 AssistantMessage assistantMessage = result.getOutput();
                                 if (assistantMessage != null) {
                                     // 发送文本内容，处理 <think> 标签
                                     String text = assistantMessage.getText();
                                     if (text != null && !text.isEmpty()) {
                                         flux = Flux.concat(flux, parseTextWithThink(text));
                                     }
                                 }
                             }

                             return flux;
                         })
                         .doFinally(signal -> ToolCallEventCollector.clear(conversationId));

        // 合并工具事件和聊天响应流，实时发送
        return Flux.merge(toolEventFlux, chatFlux)
                   .concatWith(Flux.just("[DONE]"));
    }
}
