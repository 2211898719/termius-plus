package com.codeages.termiusplus.biz.patrol.agent;

import com.codeages.termiusplus.biz.patrol.agent.tool.*;
import com.codeages.termiusplus.biz.patrol.service.AgentLlmClientFactory;
import com.codeages.termiusplus.security.AuthUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class PatrolAgentService {

    private final CleanupTool cleanupTool;
    private final AgentLlmClientFactory factory;
    private final DiskTool diskTool;
    private final DockerTool dockerTool;
    private final ExecuteCommandTool executeCommandTool;
    private final NginxTool nginxTool;
    private final ServiceTool serviceTool;
    private final ServerTool serverTool;
    private final PatrolScriptTool patrolScriptTool;
    private final ChatMemory chatMemory;

    private static final String SYSTEM_PROMPT = """
            你是一个 Linux 服务器运维 AI 助手。你可以帮助用户检查和解决服务器问题。
            你可以使用工具来执行命令、检查磁盘、分析 nginx 配置、管理 docker 容器等。
            重要规则：
            1. 只能使用提供的工具，不要自行构造任意命令
            2. 对于危险操作（删除文件、重启服务、重启/删除 docker 容器等），必须先告知用户并获得确认
            3. 先收集信息再分析，给出清晰的问题诊断和解决方案
            4. 如果用户要求执行危险操作，先用 cleanupFiles / listContainers 等工具列出将要影响的对象，等用户确认后再调用 confirmXxx 等工具
            5. 输出使用 Markdown 格式，便于前端展示
            6. 当用户希望把某个检查逻辑沉淀为可重复执行的巡检任务时，用 createPatrolScript 把脚本保存到巡检脚本库。
            7. 若脚本只适用于部分服务器或某些分组（而不是所有服务器），通过 serverIds / groupIds 参数限定范围；都不传则适用于所有服务器。

            关于命令确认的硬性规则（必须严格遵守）：
            - executeCommand 对不在白名单的命令会返回 "[需要用户确认] 命令 "X" 不在白名单中"，此时该命令并未真正执行。
            - 用户回复"确认"或"确认执行"代表确认执行**所有**此前被拒绝的、待确认的命令，不是一个一个确认。
            - 用户确认后，必须**逐个**用 executeDangerousCommand 重新调用此前所有返回过 [需要用户确认] 的命令，
              不得遗漏、不得改用 executeCommand、不得只确认其中一部分。
            - 如果只确认部分命令，用户日志中仍会看到 "[需要用户确认]"，视为未完成。

            Docker 工具使用规则：
            - 只读工具(listContainers / listImages / getContainerStats / getContainerLogs / inspectContainer /
              getDockerInfo / getDiskUsage / listNetworks / listVolumes)直接调用即可。
            - 写操作工具(startContainer / stopContainer / restartContainer / removeContainer / pullImage /
              runContainer / execInContainer)只会返回 [需要用户确认] 提示，不会真正执行。
            - 用户确认后，**必须**调用对应的 confirmXxx 系列方法才能真正执行，
              不得跳过确认直接调 confirm，也不得调错对象（比如用户同意 start 就不该顺便 rm）。
            - 删除容器是高危操作，默认 force=false；如果用户明确说"强制删"才传 true。
            - 容器名/镜像名如果带特殊字符或格式异常，应拒绝执行并提示用户。
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
        AuthUser user = currentUser();
        PatrolPermissionService.setCurrentUser(user);
        try {
            return buildChatClient(conversationId).prompt(SYSTEM_PROMPT)
                             .tools(cleanupTool, diskTool, dockerTool, executeCommandTool, nginxTool, serviceTool, serverTool, patrolScriptTool)
                             .user(userMessage)
                             .call()
                             .content();
        } finally {
            PatrolPermissionService.clear();
        }
    }

    public Flux<String> stream(String userMessage, String conversationId) {
        // 创建统一的事件 sink（工具事件、AI 响应、[DONE] 都通过它发送）
        Sinks.Many<String> sink = ToolCallEventCollector.createSink(conversationId);

        // 工具运行在 Reactor 调度线程上，SecurityContextHolder 的 ThreadLocal 不会跟随。
        // 把当前用户快照传到 ThreadLocal 上，让 PatrolPermissionService 在工具线程上能读到。
        AuthUser user = currentUser();
        PatrolPermissionService.setCurrentUser(user);

        // 设置工具的 sink，使工具事件直接发送到统一 sink
        cleanupTool.setSink(sink);
        diskTool.setSink(sink);
        dockerTool.setSink(sink);
        executeCommandTool.setSink(sink);
        nginxTool.setSink(sink);
        serviceTool.setSink(sink);
        serverTool.setSink(sink);
        patrolScriptTool.setSink(sink);

        // 跟踪工具执行状态
        AtomicInteger activeToolCount = new AtomicInteger(0);
        AtomicBoolean chatDone = new AtomicBoolean(false);

        // AI 响应流 - 启动异步执行，文本和工具事件都发送到 sink
        buildChatClient(conversationId).prompt(SYSTEM_PROMPT)
                         .tools(cleanupTool, diskTool, dockerTool, executeCommandTool, nginxTool, serviceTool, serverTool, patrolScriptTool)
                         .user(userMessage)
                         .stream()
                         .content()
                         .doOnNext(text -> sink.tryEmitNext("text:" + text))
                         .doOnComplete(() -> {
                             chatDone.set(true);
                             if (activeToolCount.get() == 0) {
                                 sink.tryEmitNext("[DONE]");
                             }
                         })
                         .doFinally(signal -> PatrolPermissionService.clear())
                         .subscribe();

        // 返回 sink 流，在 doOnNext 中跟踪工具事件以发送 [DONE]
        return sink.asFlux()
                   .doOnNext(event -> {
                       if (event.startsWith("tool_event:")) {
                           if (event.contains("\"tool_start\"")) {
                               activeToolCount.incrementAndGet();
                           } else if (event.contains("\"tool_result\"")) {
                               if (activeToolCount.decrementAndGet() == 0 && chatDone.get()) {
                                   sink.tryEmitNext("[DONE]");
                               }
                           }
                       }
                   })
                   .doFinally(signal -> ToolCallEventCollector.clear(conversationId));
    }

    private AuthUser currentUser() {
        try {
            var auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth == null || !(auth.getPrincipal() instanceof AuthUser user)) {
                return null;
            }
            return user;
        } catch (Exception e) {
            return null;
        }
    }
}
