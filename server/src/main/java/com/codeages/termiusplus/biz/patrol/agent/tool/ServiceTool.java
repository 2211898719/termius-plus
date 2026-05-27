package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ServiceTool {

    @Tool("检查指定服务（如 nginx, mysql, redis 等）的运行状态。参数 serverId 是服务器ID，serviceName 是服务名称。")
    public String getServiceStatus(Long serverId, String serviceName) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl status " + serviceName + " 2>&1");
        } catch (Exception e) {
            return "检查失败: " + e.getMessage();
        }
    }

    @Tool("列出所有正在运行的服务。参数 serverId 是服务器ID。")
    public String listRunningServices(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("systemctl list-units --type=service --state=running --no-pager");
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }
}
