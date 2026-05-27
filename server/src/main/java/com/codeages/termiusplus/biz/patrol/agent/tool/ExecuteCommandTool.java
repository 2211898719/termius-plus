package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.config.CommandWhitelistConfig;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteCommandTool {

    private final CommandWhitelistConfig whitelistConfig;

    @Tool("在指定服务器上执行 shell 命令。如果命令在白名单中会自动执行，否则返回需要用户确认的提示。参数 serverId 是服务器ID，command 是要执行的命令。")
    public String executeCommand(Long serverId, String command) {
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

    @Tool("执行需要用户确认的危险命令（如 rm, systemctl restart, reboot 等）。必须先获得用户明确确认后才能调用此工具。")
    public String executeDangerousCommand(Long serverId, String command) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand(command);
        } catch (Exception e) {
            log.error("执行危险命令失败: serverId={}, command={}", serverId, command, e);
            return "执行失败: " + e.getMessage();
        }
    }
}
