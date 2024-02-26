package com.codeages.javaskeletonserver.biz.server.ws.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import com.codeages.javaskeletonserver.biz.user.dto.UserDto;
import com.codeages.javaskeletonserver.security.AuthTokenFilter;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.*;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static com.codeages.javaskeletonserver.biz.server.context.ServerContext.SSH_POOL;

/**
 * sessionId用来标识客户端，用于拉起键盘验证授权识别客户端使用
 * serverId用来标识连接的服务器
 * masterSessionId用来标识是否为主连接，如果是0，说明是主连接
 */
@Slf4j
@Component
@ServerEndpoint("/socket/ssh/{sessionId}/{serverId}/{masterSessionId}")
public class SshHandler {

    private static final int MAX_LOG_BUFFER_SIZE = 1024 * 1024 * 5;

    private static final String NONE_MASTER_SESSION_ID = "0";

    private final ServerService serverService;

    private final CommandLogService commandLogService;

    public SshHandler() {
        this.serverService = SpringUtil.getBean(ServerService.class);
        this.commandLogService = SpringUtil.getBean(CommandLogService.class);
    }

    @OnOpen
    public void onOpen(javax.websocket.Session session,
                       @PathParam("sessionId") String sessionId,
                       @PathParam("serverId") Long serverId,
                       @PathParam("masterSessionId") String masterSessionId) {
        Long userId = AuthTokenFilter.userIdThreadLocal.get();
        // 为了防止内存泄漏，这里需要手动清理
        AuthTokenFilter.userIdThreadLocal.remove();

        log.info("有连接加入,sessionId={}", session.getId());

        // 如果是主连接 0，就创建一个新的连接
        if (isMasterSession(masterSessionId)) {
            HandlerItem handlerItem = new HandlerItem(
                    userId,
                    serverId,
                    session,
                    serverService.createSSHClient(serverId, sessionId)
            );

            log.info("创建连接成功：{}", session.getId());
            SSH_POOL.put(session.getId(), handlerItem);
        } else {
            // 如果不是0，说明是子连接，找到主连接，然后加入到主连接的sessions中
            HandlerItem handlerItem = SSH_POOL.get(masterSessionId);
            if (handlerItem == null) {
                log.error("主连接不存在：{}", masterSessionId);
                sendBinary(session, "主连接不存在或已关闭");
                return;
            }

            handlerItem.addSubSession(session);
        }
    }


    @OnMessage
    public void onMessage(@PathParam("masterSessionId") String masterSessionId,
                          String message,
                          javax.websocket.Session session) {
        HandlerItem handlerItem = SSH_POOL.get(session.getId());
        if (!isMasterSession(masterSessionId)) {
            handlerItem = SSH_POOL.get(masterSessionId);
        }

        if (handlerItem == null) {
            log.error("主连接不存在：{}", session.getId());
            return;
        }

        //如果是事件，就不发送到服务器
        MessageDto messageDto = MessageDto.parse(message);
        switch (messageDto.getEvent()) {
            case COMMAND:
                sendCommand(handlerItem, messageDto.getData());
                break;
            case RESIZE:
                ResizeDto resizeDto = JSONUtil.toBean(messageDto.getData(), ResizeDto.class);
                handlerItem.reSize(
                        resizeDto.getCols(),
                        resizeDto.getRows(),
                        resizeDto.getWidth(),
                        resizeDto.getHeight()
                );
                break;
            default:
                log.error("未知事件：{}", messageDto.getEvent());
                break;
        }
    }

    @OnClose
    public void onClose(javax.websocket.Session session,
                        @PathParam("masterSessionId") String masterSessionId) {
        if (isMasterSession(masterSessionId)) {
            HandlerItem handlerItem = SSH_POOL.get(session.getId());
            handlerItem.close();
            SSH_POOL.remove(session.getId());
        } else {
            HandlerItem handlerItem = SSH_POOL.get(masterSessionId);
            if (handlerItem != null) {
                handlerItem.removeSubSession(session);
            }
        }

        log.info("有连接关闭,sessionId={}", session.getId());
    }

    @OnError
    public void onError(javax.websocket.Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());

        if (error instanceof UserAuthException) {
            sendBinary(session, "用户认证错误");
        } else if (error instanceof ConnectException) {
            sendBinary(session, "网络连接错误，检查ip和端口是否正确");
        } else {
            sendBinary(session, "发生错误：" + error.getMessage());
        }

        error.printStackTrace();
    }

    private void sendCommand(HandlerItem handlerItem, String data) {
        try {
            handlerItem.outputStream.write(data.getBytes());
            handlerItem.outputStream.flush();
        } catch (IOException e) {
            log.error("发送命令失败：{}", e.getMessage());
        }

    }


    private static void sendBinary(javax.websocket.Session session, String msg) {
        if (!session.isOpen()) {
            // 会话关闭不能发送消息
            return;
        }

        synchronized (session.getId().intern()) {
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                log.error("发送消息出错：{}", e.getMessage());
            }
        }
    }

    private boolean isMasterSession(String masterSessionId) {
        return NONE_MASTER_SESSION_ID.equals(masterSessionId);
    }

    @SneakyThrows
    public void destroy(javax.websocket.Session session) {
        HandlerItem handlerItem = SSH_POOL.get(session.getId());
        if (handlerItem != null) {
            handlerItem.close();
        }

        IoUtil.close(session);
        SSH_POOL.remove(session.getId());
    }


    public class HandlerItem implements Runnable {
        @Getter
        private final Long userId;
        @Getter
        private final Long serverId;
        @Getter
        @Setter
        private UserDto userDto;
        @Getter
        private final String masterSessionId;
        private final List<Session> sessions = new CopyOnWriteArrayList<>();
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final net.schmizz.sshj.connection.channel.direct.Session openSession;
        private final net.schmizz.sshj.connection.channel.direct.Session.Shell shell;

        private final File logFile;
        private final BufferedOutputStream logFileOutputStream;

        @SneakyThrows
        HandlerItem(Long userId,
                    Long serverId,
                    javax.websocket.Session session,
                    SSHClient sshClient) {

            this.sessions.add(session);
            this.userId = userId;
            this.serverId = serverId;

            this.masterSessionId = session.getId();

            sshClient.useCompression();
            net.schmizz.sshj.connection.channel.direct.Session shellSession = sshClient.startSession();
            shellSession.allocatePTY("xterm", 80, 24, 640, 480, Map.of());
            shellSession.setAutoExpand(true);

            this.shell = shellSession.startShell();

            this.inputStream = shell.getInputStream();
            this.outputStream = shell.getOutputStream();

            //auto sudo
            ServerDto serverDto = serverService.findById(serverId);
            if (Boolean.TRUE.equals(serverDto.getAutoSudo()) && ObjectUtil.notEqual(serverDto.getUsername(), "root")) {
                outputStream.write(("echo " + serverDto.getPassword() + " | sudo -S ls && sudo -i\n").getBytes());
                outputStream.flush();
            }

            if (CharSequenceUtil.isNotBlank(serverDto.getFirstCommand())){
                outputStream.write((serverDto.getFirstCommand()).getBytes());
                outputStream.flush();
            }

            this.openSession = shellSession;

            CommandLogDto commandLogDto = commandLogService
                                                    .create(new CommandLogCreateParams(
                                                            userId,
                                                            session.getId(),
                                                            serverId
                                                    ));
            log.info("创建命令记录成功：{}", commandLogDto.getId());
            logFile = FileUtil.file(commandLogDto.getCommandData());

            //日志内存缓冲区
            logFileOutputStream = IoUtil.toBuffered(
                    FileUtil.getOutputStream(logFile),
                    MAX_LOG_BUFFER_SIZE
            );

            ThreadUtil.execute(this);
        }


        public void reSize(int col, int row, int width, int height) {
            try {
                shell.changeWindowDimensions(col, row, width, height);
            } catch (IOException e) {
                log.error("调整窗口大小失败：{}", e.getMessage());
            }
        }

        public void close() {
            try {
                IoUtil.close(this.inputStream);
                IoUtil.close(this.outputStream);
                IoUtil.close(this.logFileOutputStream);
                this.shell.close();
                this.openSession.close();
            } catch (IOException e) {
                log.error("关闭连接失败：{}", e.getMessage());
            }
        }

        @SneakyThrows
        public synchronized void addSubSession(javax.websocket.Session session) {
            this.sessions.add(session);

            //写入内存缓冲区中的数据到文件
            logFileOutputStream.flush();

            MessageDto messageDto = new MessageDto(EventType.COMMAND, FileUtil.readString(logFile, openSession.getRemoteCharset()));
            String s = JSONUtil.toJsonStr(messageDto);

            //把之前的服务器上下文的数据发送给新的子连接
            sendBinary(session, s);
        }

        @SneakyThrows
        public void removeSubSession(javax.websocket.Session session) {
            this.sessions.remove(session);
            IoUtil.close(session);
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != -1) {
                    String originData = new String(Arrays.copyOfRange(buffer, 0, i), openSession.getRemoteCharset());
                    MessageDto messageDto = new MessageDto(EventType.COMMAND, originData);
                    String s = JSONUtil.toJsonStr(messageDto);
                    logFileOutputStream.write(originData.getBytes());
                    sessions.forEach(session -> sendBinary(session, s));
                }
            } catch (Exception e) {
                if (!this.openSession.isOpen()) {
                    return;
                }
                sessions.forEach(SshHandler.this::destroy);
            }
        }
    }
}
