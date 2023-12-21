package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.net.NetUtil;
import cn.hutool.core.thread.ThreadUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.dto.PortForwarderDto;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.service.PortForWardingService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.exception.AppException;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.direct.Parameters;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder;
import net.schmizz.sshj.connection.channel.forwarded.SocketForwardingConnectListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class PortForWardingServiceImpl implements PortForWardingService {

    private final ServerService serverService;

    private final HttpServletRequest httpServletRequest;

    private final ServerSocket serverSocket;

    private final Map<Integer, PortForwarderDto> localPortForwarderMap;

    @Value("${current.ip}")
    private String currentIp;

    public PortForWardingServiceImpl(ServerService serverService,
                                     HttpServletRequest httpServletRequest,
                                     ServerSocket serverSocket) {
        this.serverService = serverService;
        this.httpServletRequest = httpServletRequest;
        this.serverSocket = serverSocket;
        this.localPortForwarderMap = new ConcurrentHashMap<>();
    }

    @Override
    public List<PortForwarderDto> list() {
        return List.copyOf(localPortForwarderMap.values());
    }

    /**
     * 将本地端口转发到远程端口，使用本地host
     *
     * @param localPort
     * @param serverId
     * @param remotePort
     */
    @SneakyThrows
    @Override
    public void startPortForwarding(String forwardingName, Integer localPort, Long serverId, Integer remotePort) {
        if (localPortForwarderMap.containsKey(localPort)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口已被“" + localPortForwarderMap.get(localPort)
                                                                                                .getForwardingName() + "”映射占用");
        }

        if (!NetUtil.isUsableLocalPort(localPort)) {
            throw new AppException(ErrorCode.INTERNAL_ERROR, "端口已被其他应用占用");
        }

        ServerDto serverDto = serverService.findById(serverId);

        LocalPortForwarder localPortForwarder = serverService.createSSHClient(serverId)
                                                             .newLocalPortForwarder(new Parameters(
                                                                     currentIp,
                                                                     localPort,
                                                                     serverDto.getIp(),
                                                                     remotePort
                                                             ), new ServerSocket(localPort));

        CompletableFuture.runAsync(() -> {
            try {
                localPortForwarder.listen(ThreadUtil.newThread(() -> {
                }, "测试"));
            } catch (IOException e) {
                log.error("端口转发失败", e);
                try {
                    localPortForwarder.close();
                } catch (IOException ex) {
                    throw new RuntimeException(ex);
                }
                localPortForwarderMap.remove(localPort);
            }
        });


        localPortForwarderMap.put(
                localPort,
                new PortForwarderDto(
                        forwardingName,
                        localPortForwarder,
                        localPort,
                        currentIp,
                        remotePort,
                        serverId,
                        serverDto
                )
        );
    }

    @SneakyThrows
    @Override
    public void stopPortForwarding(Integer localPort) {
        localPortForwarderMap.get(localPort).getLocalPortForwarder().close();
        localPortForwarderMap.remove(localPort);
    }

    @Override
    public boolean isRunning(Integer localPort) {
        return localPortForwarderMap.containsKey(localPort) ;
    }

    @Override
    public void stopAllPortForwarding() {
        localPortForwarderMap.forEach((k, v) -> {
            try {
                   v.getLocalPortForwarder().close();
            } catch (IOException e) {
                log.error("关闭端口转发失败", e);
            }
        });

        localPortForwarderMap.clear();
    }


}
