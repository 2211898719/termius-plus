package com.codeages.termiusplus.biz.patrol.agent.tool;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.patrol.agent.PatrolPermissionService;
import com.codeages.termiusplus.biz.patrol.agent.ToolCallHelper;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DiskTool {

    private final PatrolPermissionService permissionService;
    private Sinks.Many<String> sink;

    public void setSink(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Tool(description = "获取服务器磁盘使用情况，包括各分区的总容量、已用、可用和使用率。")
    public String getDiskUsage(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "getDiskUsage", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                List<DiskUsage> usages = client.getDiskUsage();
                return JSONUtil.toJsonStr(usages);
            } catch (Exception e) {
                return "获取失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "分析指定目录下占用空间最大的子目录，用于定位存储占用来源。")
    public String analyzeStorageUsage(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "目录路径，例如 / 或 /var") String path) {
        return ToolCallHelper.execute(sink, "analyzeStorageUsage", "serverId=" + serverId + ", path=" + path, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("du -sh " + path + "/* 2>/dev/null | sort -rh | head -20");
            } catch (Exception e) {
                return "分析失败: " + e.getMessage();
            }
        });
    }
}
