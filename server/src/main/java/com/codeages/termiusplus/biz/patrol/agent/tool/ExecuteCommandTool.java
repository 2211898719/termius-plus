package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.config.CommandWhitelistConfig;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteCommandTool {

    private final CommandWhitelistConfig whitelistConfig;

    @Tool(description = "在指定服务器上执行 shell 命令。如果命令在白名单中会自动执行，否则返回需要用户确认的提示。")
    public String executeCommand(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "要执行的命令") String command) {
        boolean autoExecute = whitelistConfig.shouldAutoExecute(command);

        if (!autoExecute) {
            return "[需要用户确认] 命令 \"" + command + "\" 不在白名单中，需要用户确认后执行。请询问用户是否确认执行。";
        }

        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand(command);
        } catch (Exception e) {
            log.error("执行命令失败: serverId={}, command={}", serverId, command, e);
            return "执行失败: " + e.getMessage();
        }
    }

    @Tool(description = "执行需要用户确认的危险命令（如 rm, systemctl restart, reboot 等）。必须先获得用户明确确认后才能调用此工具。")
    public String executeDangerousCommand(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "要执行的危险命令") String command) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand(command);
        } catch (Exception e) {
            log.error("执行危险命令失败: serverId={}, command={}", serverId, command, e);
            return "执行失败: " + e.getMessage();
        }
    }
}
