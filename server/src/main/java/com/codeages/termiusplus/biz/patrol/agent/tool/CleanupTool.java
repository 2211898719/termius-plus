package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CleanupTool {

    @Tool("清理指定路径下符合模式的旧文件（如 /tmp 下超过 7 天的文件）。此操作会删除文件，必须先获得用户确认。参数 serverId 是服务器ID，path 是路径，pattern 是文件名模式，olderThanDays 是天数。")
    public String cleanupFiles(Long serverId, String path, String pattern, int olderThanDays) {
        String command = String.format("find %s -name '%s' -mtime +%d -type f 2>/dev/null | head -50", path, pattern, olderThanDays);
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            String files = client.executeCommand(command);
            if (files.isEmpty()) {
                return "没有找到符合条件的文件";
            }
            return "以下文件将被删除（需要确认）:\n" + files;
        } catch (Exception e) {
            return "查找失败: " + e.getMessage();
        }
    }

    @Tool("确认执行文件清理。在用户确认后调用此工具实际删除文件。参数 serverId 是服务器ID，path 是路径，pattern 是文件名模式，olderThanDays 是天数。")
    public String confirmCleanup(Long serverId, String path, String pattern, int olderThanDays) {
        String command = String.format("find %s -name '%s' -mtime +%d -type f -delete 2>&1", path, pattern, olderThanDays);
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return "清理完成: " + client.executeCommand(command);
        } catch (Exception e) {
            return "清理失败: " + e.getMessage();
        }
    }
}
