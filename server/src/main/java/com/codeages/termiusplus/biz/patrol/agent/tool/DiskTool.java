package com.codeages.termiusplus.biz.patrol.agent.tool;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import com.codeages.termiusplus.biz.util.command.DiskUsage;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class DiskTool {

    @Tool("获取服务器磁盘使用情况，包括各分区的总容量、已用、可用和使用率。参数 serverId 是服务器ID。")
    public String getDiskUsage(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            List<DiskUsage> usages = client.getDiskUsage();
            return JSONUtil.toJsonStr(usages);
        } catch (Exception e) {
            return "获取失败: " + e.getMessage();
        }
    }

    @Tool("分析指定目录下占用空间最大的子目录，用于定位存储占用来源。参数 serverId 是服务器ID，path 是目录路径如 / 或 /var。")
    public String analyzeStorageUsage(Long serverId, String path) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("du -sh " + path + "/* 2>/dev/null | sort -rh | head -20");
        } catch (Exception e) {
            return "分析失败: " + e.getMessage();
        }
    }
}
