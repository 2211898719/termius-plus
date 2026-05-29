package com.codeages.termiusplus.biz.patrol.agent;

import com.codeages.termiusplus.biz.patrol.agent.tool.*;
import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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

    public Flux<String> stream(String userMessage, String conversationId) {
        return buildChatClient(conversationId).prompt(SYSTEM_PROMPT)
                         .tools(cleanupTool, diskTool, executeCommandTool, nginxTool, serviceTool, serverTool)
                         .user(userMessage)
                         .stream()
                         .content()
                         .concatWith(Flux.just("[DONE]"));
    }
}
