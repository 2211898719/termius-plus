package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceTool {

    @Tool(description = "检查指定服务（如 nginx, mysql, redis 等）的运行状态。")
    public String getServiceStatus(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "服务名称，例如 nginx、mysql 或 redis") String serviceName) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl status " + serviceName + " 2>&1");
        } catch (Exception e) {
            return "检查失败: " + e.getMessage();
        }
    }

    @Tool(description = "列出所有正在运行的服务。")
    public String listRunningServices(@ToolParam(description = "服务器ID") Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl list-units --type=service --state=running --no-pager");
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }
}
