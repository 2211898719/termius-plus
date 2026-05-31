package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.patrol.agent.ToolCallHelper;
import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Sinks;

@Slf4j
@Component
public class NginxTool {

    private Sinks.Many<String> sink;

    public void setSink(Sinks.Many<String> sink) {
        this.sink = sink;
    }

    @Tool(description = "获取服务器上所有 nginx 站点的 SSL 证书信息，包括域名、到期时间、颁发者等。")
    public String getNginxCerts(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "getNginxCerts", "serverId=" + serverId, () -> {
            String script = "for conf in /etc/nginx/sites-enabled/* /etc/nginx/conf.d/*.conf; do "
                    + "[ -f \"$conf\" ] || continue; "
                    + "domain=$(basename \"$conf\" .conf); "
                    + "ssl_cert=$(grep -oP 'ssl_certificate\\s+\\K[^;]+' \"$conf\" 2>/dev/null | head -1); "
                    + "if [ -n \"$ssl_cert\" ] && [ -f \"$ssl_cert\" ]; then "
                    + "echo \"{\\\"domain\\\":\\\"$domain\\\",\\\"cert\\\":\\\"$ssl_cert\\\","
                    + "\\\"info\\\":\\\"$(openssl x509 -in \"$ssl_cert\" -noout -subject -issuer -dates 2>/dev/null | tr '\\n' '|')\\\"}\"; "
                    + "fi; done";

            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand(script);
            } catch (Exception e) {
                log.error("获取 nginx 证书信息失败: serverId={}", serverId, e);
                return "获取失败: " + e.getMessage();
            }
        });
    }

    @Tool(description = "检查 nginx 配置是否正确。")
    public String checkNginxConfig(@ToolParam(description = "服务器ID") Long serverId) {
        return ToolCallHelper.execute(sink, "checkNginxConfig", "serverId=" + serverId, () -> {
            try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
                return client.executeCommand("nginx -t 2>&1");
            } catch (Exception e) {
                return "检查失败: " + e.getMessage();
            }
        });
    }
}
