package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.agent.PatrolPermissionService;
import com.codeages.termiusplus.biz.patrol.agent.ToolCallHelper;
import com.codeages.termiusplus.biz.patrol.config.CommandWhitelistConfig;
import com.codeages.termiusplus.biz.patrol.entity.PatrolCommandLog;
import com.codeages.termiusplus.biz.patrol.repository.PatrolCommandLogRepository;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExecuteCommandTool {

    private static final int OUTPUT_MAX_LENGTH = 4096;

    private final CommandWhitelistConfig whitelistConfig;
    private final PatrolCommandLogRepository commandLogRepository;
    private final PatrolPermissionService permissionService;
    private Sinks.Many<String> sink;

    public void setSink(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Tool(description = "在指定服务器上执行 shell 命令。如果命令在白名单中会自动执行，否则返回需要用户确认的提示。")
    public String executeCommand(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "要执行的命令") String command) {
        return ToolCallHelper.execute(sink, "executeCommand", "serverId=" + serverId + ", command=" + command, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            boolean autoExecute = whitelistConfig.shouldAutoExecute(command);

            if (!autoExecute) {
                String result = "[需要用户确认] 命令 \"" + command + "\" 不在白名单中，需要用户确认后执行。请询问用户是否确认执行。";
                saveLog(serverId, command, PatrolCommandLog.TYPE_CONFIRM_PENDING, result);
                return result;
            }

            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String result = client.executeCommand(command);
                saveLog(serverId, command, PatrolCommandLog.TYPE_AUTO, result);
                return result;
            } catch (Exception e) {
                log.error("执行命令失败: serverId={}, command={}", serverId, command, e);
                String err = "执行失败: " + e.getMessage();
                saveLog(serverId, command, PatrolCommandLog.TYPE_AUTO, err);
                return err;
            }
        });
    }

    @Tool(description = "执行需要用户确认的危险命令（如 rm, systemctl restart, reboot 等）。必须先获得用户明确确认后才能调用此工具。")
    public String executeDangerousCommand(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "要执行的危险命令") String command) {
        return ToolCallHelper.execute(sink, "executeDangerousCommand", "serverId=" + serverId + ", command=" + command, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String result = client.executeCommand(command);
                saveLog(serverId, command, PatrolCommandLog.TYPE_DANGEROUS, result);
                return result;
            } catch (Exception e) {
                log.error("执行危险命令失败: serverId={}, command={}", serverId, command, e);
                String err = "执行失败: " + e.getMessage();
                saveLog(serverId, command, PatrolCommandLog.TYPE_DANGEROUS, err);
                return err;
            }
        });
    }

    private void saveLog(Long serverId, String command, String execType, String output) {
        // 系统日志
        if (PatrolCommandLog.TYPE_CONFIRM_PENDING.equals(execType)) {
            log.warn("[AI 巡查][需确认] serverId={}, command={}", serverId, command);
        } else if (PatrolCommandLog.TYPE_DANGEROUS.equals(execType)) {
            log.info("[AI 巡查][已确认执行] serverId={}, command={}", serverId, command);
        } else {
            log.info("[AI 巡查][自动执行] serverId={}, command={}", serverId, command);
        }
        // 落库
        try {
            PatrolCommandLog record = new PatrolCommandLog();
            record.setServerId(serverId);
            record.setCommand(command);
            record.setExecType(execType);
            record.setOutput(truncate(output));
            commandLogRepository.save(record);
        } catch (Exception e) {
            log.error("保存命令执行日志失败: serverId={}, command={}, type={}", serverId, command, execType, e);
        }
    }

    private String truncate(String s) {
        if (s == null) return null;
        return s.length() > OUTPUT_MAX_LENGTH ? s.substring(0, OUTPUT_MAX_LENGTH) : s;
    }
}
