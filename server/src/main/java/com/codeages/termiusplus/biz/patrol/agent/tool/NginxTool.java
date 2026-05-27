package com.codeages.termiusplus.biz.patrol.agent.tool;

import com.codeages.termiusplus.biz.util.ExecuteCommandSSHClient;
import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NginxTool {

    @Tool("获取服务器上所有 nginx 站点的 SSL 证书信息，包括域名、到期时间、颁发者等。参数 serverId 是服务器ID。")
    public String getNginxCerts(Long serverId) {
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
    }

    @Tool("检查 nginx 配置是否正确。参数 serverId 是服务器ID。")
    public String checkNginxConfig(Long serverId) {
        try (ExecuteCommandSSHClient client = new ExecuteCommandSSHClient(serverId)) {
            return client.executeCommand("nginx -t 2>&1");
        } catch (Exception e) {
            return "检查失败: " + e.getMessage();
        }
    }
}
