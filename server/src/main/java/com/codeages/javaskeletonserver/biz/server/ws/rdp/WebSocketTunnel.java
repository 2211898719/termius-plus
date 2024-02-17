package com.codeages.javaskeletonserver.biz.server.ws.rdp;


import cn.hutool.extra.spring.SpringUtil;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.enums.OSEnum;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import com.codeages.javaskeletonserver.biz.user.service.UserService;
import com.codeages.javaskeletonserver.security.AuthTokenFilter;
import org.apache.guacamole.GuacamoleException;
import org.apache.guacamole.net.GuacamoleSocket;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleClientInformation;
import org.apache.guacamole.protocol.GuacamoleConfiguration;
import org.apache.guacamole.websocket.GuacamoleWebSocketTunnelEndpoint;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.websocket.EndpointConfig;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@Component
@ServerEndpoint(value = "/socket/{protocol}/{serverId}")
public class WebSocketTunnel extends GuacamoleWebSocketTunnelEndpoint {

    private static String host;
    private static int port;
    private static String guacamoleMapping;
    private static String serverName;

    @Value("${guacamole.host:127.0.0.1}")
    public void setHost(String host) {
        WebSocketTunnel.host = host;
    }

    @Value("${guacamole.port:4822}")
    public void setPort(int port) {
        WebSocketTunnel.port = port;
    }

    @Value("${guacamole.mapping:/tmp}")
    public void setGuacamoleMapping(String guacamoleMapping) {
        WebSocketTunnel.guacamoleMapping = guacamoleMapping;
    }

    @Value("${spring.application.name:file}")
    public void setServerName(String serverName) {
        WebSocketTunnel.serverName = serverName;
    }

    /**
     * Returns a new tunnel for the given session. How this tunnel is created
     * or retrieved is implementation-dependent.
     *
     * @param session        The session associated with the active WebSocket
     *                       connection.
     * @param endpointConfig information associated with the instance of
     *                       the endpoint created for handling this single connection.
     * @return A connected tunnel, or null if no such tunnel exists.
     * @throws GuacamoleException If an error occurs while retrieving the
     *                            tunnel, or if access to the tunnel is denied.
     */
    @Override
    protected GuacamoleTunnel createTunnel(Session session, EndpointConfig endpointConfig) throws GuacamoleException {
        Long userId = AuthTokenFilter.userIdThreadLocal.get();
        // 为了防止内存泄漏，这里需要手动清理
        AuthTokenFilter.userIdThreadLocal.remove();
        UserDto userDto = SpringUtil.getBean(UserService.class)
                                       .get(userId)
                                       .orElseThrow(() -> new GuacamoleException("用户不存在"));

        // 获取url的值
        int height = Integer.parseInt(session.getRequestParameterMap().get("height").get(0));
        int width = Integer.parseInt(session.getRequestParameterMap().get("width").get(0));
        GuacamoleClientInformation information = new GuacamoleClientInformation();
        information.setOptimalScreenHeight(height);
        information.setOptimalScreenWidth(width);

        Long serverId = Long.parseLong(session.getPathParameters().get("serverId"));
        String protocol = session.getPathParameters().get("protocol");
        ServerDto serverDto = SpringUtil.getBean(ServerService.class).findById(serverId);
        if (serverDto == null) {
            throw new GuacamoleException("服务器不存在");
        }
        if (!OSEnum.WINDOWS.equals(serverDto.getOs())) {
            throw new GuacamoleException("不支持的操作系统");
        }
        //--------------------------windows远程桌面测试--------------------------
        GuacamoleConfiguration configuration = new GuacamoleConfiguration();
        configuration.setProtocol(protocol);
        configuration.setParameter("hostname", serverDto.getIp());
        configuration.setParameter("port", String.valueOf(serverDto.getPort()));
        configuration.setParameter("username", serverDto.getUsername());
        configuration.setParameter("password", serverDto.getPassword());
        //忽略证书
        configuration.setParameter("ignore-cert", "true");
        configuration.setParameter("security", "any");
        configuration.setParameter("resize-method", "display-update");
        //声音播放
        configuration.setParameter("enable-audio", "true");
        //允许传输文件
        configuration.setParameter("enable-drive", "true");
        //允许剪贴板
        configuration.setParameter("enable-printing", "true");
        configuration.setParameter("disable-audio", "false");
        configuration.setParameter("enable-printing", "true");
        configuration.setParameter("drive-name", userDto.getUsername());
        configuration.setParameter("drive-path", guacamoleMapping);


        //domain 域
        //security 安全模式 any-任意  nla-网络级别认证  nla-ext-扩展网络级身份验证  tls-TLS加密  vmconnect-Hyper-V / VMConnect  rdp-RDP加密
        //disable-auth 禁用认证 true false
        //显示设置
        //color-depth 色彩深度 8-256色 16-低色（16位） 24-真彩（24位） 32-真彩（32位）
        //width 宽
        //height 高
        //dpi 分辨率
        //resize-method 缩放方法 display-update-显示更新  reconnect-重新连接
        //read-only 只读 true false

        //vnc 参考地址https://guacamole.apache.org/doc/gug/configuring-guacamole.html#vnc
        //autoretry 在放弃并返回错误之前重试连接的次数
        //显示设置
        //color-depth 色彩深度 8-256色 16-低色（16位） 24-真彩（24位） 32-真彩（32位）
        //swap-red-blue 交换红/蓝成分 true false
        //cursor 光标 local-本地 remote-远程
        //read-only 只读 true false
        //force-lossless true false
        //代理设置
        //dest-host
        //dest-port

        //--------------------------windows远程桌面测试--------------------------

        GuacamoleSocket socket = new ConfiguredGuacamoleSocket(
                new InetGuacamoleSocket(host, port),
                configuration,
                information
        );

        return new SimpleGuacamoleTunnel(socket);
    }
}
