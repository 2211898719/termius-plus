package com.codeages.termiusplus.biz.patrol.agent.tool;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.patrol.agent.PatrolPermissionService;
import com.codeages.termiusplus.biz.patrol.agent.ToolCallHelper;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class DockerTool {

    /** 容器名/镜像名允许的字符:字母数字开头,后续可含 . _ - */
    private static final Pattern NAME_PATTERN = Pattern.compile("^[a-zA-Z0-9][a-zA-Z0-9_.\\-]*$");

    private final PatrolPermissionService permissionService;
    private Sinks.Many<String> sink;

    public void setSink(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    // ============ 只读工具 ============

    @Tool(description = "列出服务器上所有 docker 容器(包括已停止的),返回 JSON 数组。包含容器ID、名称、镜像、状态、端口、创建时间等。")
    public String listContainers(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "listContainers", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String out = client.executeCommand("docker ps -a --format '{{json .}}' 2>&1");
                return wrapJsonArray(out, "container");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "列出服务器上所有 docker 镜像,返回 JSON 数组。包含仓库、标签、镜像ID、创建时间、大小。")
    public String listImages(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "listImages", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String out = client.executeCommand("docker images --format '{{json .}}' 2>&1");
                return wrapJsonArray(out, "image");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "获取所有运行中容器的实时资源占用(CPU、内存、网络、磁盘),返回 JSON 数组。")
    public String getContainerStats(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "getContainerStats", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String out = client.executeCommand("docker stats --no-stream --format '{{json .}}' 2>&1");
                return wrapJsonArray(out, "stat");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "获取指定容器的最近若干行日志(原始文本输出)。")
    public String getContainerLogs(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName,
            @ToolParam(description = "最近多少行日志,默认 100") int tail) {
        return ToolCallHelper.execute(sink, "getContainerLogs", "serverId=" + serverId + ", containerName=" + containerName + ", tail=" + tail, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法,只允许字母数字和 . _ - ";
            }
            int n = tail <= 0 ? 100 : Math.min(tail, 1000);
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("docker logs --tail " + n + " " + containerName + " 2>&1");
            } catch (Exception e) {
                return "获取日志失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "查看指定容器的详细元数据(配置、网络、挂载、状态等),原始 JSON 文本。")
    public String inspectContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return ToolCallHelper.execute(sink, "inspectContainer", "serverId=" + serverId + ", containerName=" + containerName, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("docker inspect " + containerName + " 2>&1");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "获取 docker 引擎信息(版本、存储驱动、镜像数、容器数等),原始文本输出。")
    public String getDockerInfo(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "getDockerInfo", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("docker info 2>&1");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "获取 docker 的磁盘占用情况(镜像、容器、卷、构建缓存),原始文本输出。")
    public String getDockerDiskUsage(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "getDockerDiskUsage", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("docker system df 2>&1");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "列出服务器上所有 docker 网络,返回 JSON 数组。")
    public String listNetworks(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "listNetworks", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String out = client.executeCommand("docker network ls --format '{{json .}}' 2>&1");
                return wrapJsonArray(out, "network");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "列出服务器上所有 docker 卷,返回 JSON 数组。")
    public String listVolumes(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "listVolumes", "serverId=" + serverId, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String out = client.executeCommand("docker volume ls --format '{{json .}}' 2>&1");
                return wrapJsonArray(out, "volume");
            } catch (Exception e) {
                return "查询失败: " + e.getMessage();
            }
        });
    }

    // ============ 写操作(双重确认) ============

    @Tool(description = "【需用户确认】启动指定容器。返回 [需要用户确认] 提示,用户确认后调用 confirmStartContainer 实际执行。")
    public String startContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return ToolCallHelper.execute(sink, "startContainer", "serverId=" + serverId + ", containerName=" + containerName, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `docker start " + containerName + "`,请向用户说明影响并获得明确确认后再调用 confirmStartContainer。";
        });
    }

    @Tool(description = "【需用户确认】停止指定容器。用户确认后调用 confirmStopContainer 实际执行。")
    public String stopContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return ToolCallHelper.execute(sink, "stopContainer", "serverId=" + serverId + ", containerName=" + containerName, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `docker stop " + containerName + "`,请向用户说明影响并获得明确确认后再调用 confirmStopContainer。";
        });
    }

    @Tool(description = "【需用户确认】重启指定容器。用户确认后调用 confirmRestartContainer 实际执行。")
    public String restartContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return ToolCallHelper.execute(sink, "restartContainer", "serverId=" + serverId + ", containerName=" + containerName, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `docker restart " + containerName + "`,请向用户说明影响并获得明确确认后再调用 confirmRestartContainer。";
        });
    }

    @Tool(description = "【需用户确认】删除指定容器(可选强制删除运行中的容器)。用户确认后调用 confirmRemoveContainer 实际执行。")
    public String removeContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName,
            @ToolParam(description = "是否强制删除运行中的容器,默认 false") boolean force) {
        return ToolCallHelper.execute(sink, "removeContainer", "serverId=" + serverId + ", containerName=" + containerName + ", force=" + force, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            String cmd = "docker rm " + (force ? "-f " : "") + containerName;
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `" + cmd + "`,删除后容器数据将无法恢复,请向用户说明影响并获得明确确认后再调用 confirmRemoveContainer。";
        });
    }

    @Tool(description = "【需用户确认】从镜像仓库拉取镜像。用户确认后调用 confirmPullImage 实际执行。")
    public String pullImage(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "镜像名,例如 nginx:1.25 或 myregistry.local/myapp:v1") String image) {
        return ToolCallHelper.execute(sink, "pullImage", "serverId=" + serverId + ", image=" + image, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidImageRef(image)) {
                return "镜像名不合法,只允许字母数字和 . _ - : / ";
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `docker pull " + image + "`,请向用户说明影响并获得明确确认后再调用 confirmPullImage。";
        });
    }

    @Tool(description = "【需用户确认】以指定镜像创建并启动一个新容器。用户确认后调用 confirmRunContainer 实际执行。")
    public String runContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "新容器名") String containerName,
            @ToolParam(description = "镜像名,例如 nginx:1.25") String image,
            @ToolParam(description = "要传递给容器的命令,无则传空字符串") String command) {
        return ToolCallHelper.execute(sink, "runContainer", "serverId=" + serverId + ", containerName=" + containerName + ", image=" + image + ", command=" + command, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            if (!isValidImageRef(image)) {
                return "镜像名不合法";
            }
            StringBuilder cmd = new StringBuilder("docker run -d --name ").append(containerName).append(' ').append(image);
            if (command != null && !command.isBlank()) {
                cmd.append(' ').append(command);
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 上执行 `" + cmd + "`,请向用户说明影响并获得明确确认后再调用 confirmRunContainer。";
        });
    }

    @Tool(description = "【需用户确认】在运行中的容器内执行命令。用户确认后调用 confirmExecInContainer 实际执行。")
    public String execInContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName,
            @ToolParam(description = "要在容器内执行的命令,例如 'ls -la /etc/nginx'") String command) {
        return ToolCallHelper.execute(sink, "execInContainer", "serverId=" + serverId + ", containerName=" + containerName + ", command=" + command, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            if (!isValidName(containerName)) {
                return "容器名不合法";
            }
            if (command == null || command.isBlank()) {
                return "命令不能为空";
            }
            return "[需要用户确认] 即将在服务器 " + serverId + " 的容器 " + containerName + " 内执行 `" + command + "`,请向用户说明影响并获得明确确认后再调用 confirmExecInContainer。";
        });
    }

    // ============ confirm 二次执行方法(用户确认后实际执行) ============

    @Tool(description = "实际执行 docker start。用户已确认后调用此方法。")
    public String confirmStartContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return executeConfirmed(serverId, "start", "docker start " + containerName, containerName);
    }

    @Tool(description = "实际执行 docker stop。用户已确认后调用此方法。")
    public String confirmStopContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return executeConfirmed(serverId, "stop", "docker stop " + containerName, containerName);
    }

    @Tool(description = "实际执行 docker restart。用户已确认后调用此方法。")
    public String confirmRestartContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName) {
        return executeConfirmed(serverId, "restart", "docker restart " + containerName, containerName);
    }

    @Tool(description = "实际执行 docker rm。用户已确认后调用此方法。")
    public String confirmRemoveContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName,
            @ToolParam(description = "是否强制删除") boolean force) {
        return executeConfirmed(serverId, "remove", "docker rm " + (force ? "-f " : "") + containerName, containerName);
    }

    @Tool(description = "实际执行 docker pull。用户已确认后调用此方法。")
    public String confirmPullImage(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "镜像名") String image) {
        return executeConfirmed(serverId, "pull", "docker pull " + image, image);
    }

    @Tool(description = "实际执行 docker run -d。用户已确认后调用此方法。")
    public String confirmRunContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "新容器名") String containerName,
            @ToolParam(description = "镜像名") String image,
            @ToolParam(description = "要传递给容器的命令") String command) {
        StringBuilder sb = new StringBuilder("docker run -d --name ").append(containerName).append(' ').append(image);
        if (command != null && !command.isBlank()) {
            sb.append(' ').append(command);
        }
        return executeConfirmed(serverId, "run", sb.toString(), containerName);
    }

    @Tool(description = "实际执行 docker exec。用户已确认后调用此方法。")
    public String confirmExecInContainer(
            @ToolParam(description = "服务器ID") Long serverId,
            @ToolParam(description = "容器名或容器ID") String containerName,
            @ToolParam(description = "要在容器内执行的命令") String command) {
        return executeConfirmed(serverId, "exec", "docker exec " + containerName + " " + command, containerName);
    }

    // ============ 内部辅助方法 ============

    private String executeConfirmed(Long serverId, String opName, String command, String target) {
        return ToolCallHelper.execute(sink, "confirm" + capitalize(opName), "serverId=" + serverId + ", command=" + command, () -> {
            if (!permissionService.canAccessServer(serverId)) {
                return "无权限访问服务器 " + serverId + "，操作已拒绝。";
            }
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                String result = client.executeCommand(command);
                return "已在服务器 " + serverId + " 上执行 `" + command + "`(目标: " + target + "):\n" + result;
            } catch (Exception e) {
                log.error("Docker 操作失败: serverId={}, command={}", serverId, command, e);
                return "执行失败: " + e.getMessage();
            }
        });
    }

    /** 容器/卷/网络名:字母数字开头,可含 . _ - */
    private boolean isValidName(String name) {
        return name != null && NAME_PATTERN.matcher(name).matches();
    }

    /** 镜像引用:可含 / : @(digest) — 但不允许 shell 元字符 */
    private boolean isValidImageRef(String image) {
        if (image == null || image.isBlank()) return false;
        // 允许 [a-zA-Z0-9_.-/]:@+
        return image.matches("^[a-zA-Z0-9][a-zA-Z0-9_.\\-/:%+@]*$");
    }

    /** 把 `docker --format '{{json .}}'` 逐行输出组装为 JSON 数组 */
    private String wrapJsonArray(String raw, String entityName) {
        if (raw == null || raw.isBlank()) {
            return "[]";
        }
        // 检测错误输出
        if (raw.toLowerCase().contains("error") || raw.toLowerCase().contains("cannot connect")
                || raw.toLowerCase().contains("permission denied")) {
            return raw;
        }
        JSONArray arr = new JSONArray();
        for (String line : raw.split("\n")) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) continue;
            try {
                arr.add(JSONUtil.parse(trimmed));
            } catch (Exception e) {
                log.debug("解析 docker {} JSON 行失败: {}", entityName, trimmed);
                // 单行解析失败,跳过
            }
        }
        return arr.toString();
    }

    private static String capitalize(String s) {
        if (s == null || s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
