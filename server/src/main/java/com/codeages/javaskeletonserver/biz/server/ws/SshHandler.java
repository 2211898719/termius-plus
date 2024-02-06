package com.codeages.javaskeletonserver.biz.server.ws;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/socket/ssh/{sessionId}/{serverId}/{masterSessionId}")
public class SshHandler {

    public static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private static final AtomicInteger OnlineCount = new AtomicInteger(0);

    @OnOpen
    public void onOpen(javax.websocket.Session session,
                       @PathParam("sessionId") String sessionId,
                       @PathParam("serverId") Long serverId,
                       @PathParam("masterSessionId") String masterSessionId) {
        int cnt = OnlineCount.incrementAndGet();
        log.info("有连接加入，当前连接数为：{},sessionId={}", cnt, session.getId());
        // 如果是0，说明是主连接
        if ("0".equals(masterSessionId)) {
            HandlerItem handlerItem = new HandlerItem(
                    AuthTokenFilter.userIdThreadLocal.get(),
                    serverId,
                    session,
                    SpringUtil.getBean(ServerService.class).createSSHClient(serverId, sessionId)
            );

            log.info("创建连接成功：{}", session.getId());
            HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
        } else {
            // 如果不是0，说明是子连接，找到主连接，然后加入到主连接的sessions中
            HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(masterSessionId);
            if (handlerItem == null) {
                log.error("主连接不存在：{}", masterSessionId);
                sendBinary(session, "主连接不存在或已关闭");
                return;
            }

            handlerItem.addSubSession(session);
        }

        // 为了防止内存泄漏，这里需要手动清理
        AuthTokenFilter.userIdThreadLocal.remove();
    }

    @OnClose
    public void onClose(javax.websocket.Session session,
                        @PathParam("masterSessionId") String masterSessionId) {
        int cnt = OnlineCount.decrementAndGet();

        if (masterSessionId.equals("0")) {
            HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
            handlerItem.close();
            HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        } else {
            HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(masterSessionId);
            if (handlerItem != null) {
                handlerItem.removeSubSession(session);
            }
        }

        log.info("有连接关闭，当前连接数为：{}", cnt);
    }

    @OnMessage
    public void onMessage(@PathParam("masterSessionId") String masterSessionId,
                          String message,
                          javax.websocket.Session session) throws Exception {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        if (!"0".equals(masterSessionId)) {
            handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(masterSessionId);
        }


        if (handlerItem == null) {
            log.error("主连接不存在：{}", session.getId());
            return;
        }

        try {
            //如果是事件，就不发送到服务器
            MessageDto messageDto = MessageDto.parse(message);
            ResizeDto resizeDto = JSONUtil.toBean(messageDto.getData(), ResizeDto.class);
            handlerItem.reSize(resizeDto.getCols(), resizeDto.getRows(), resizeDto.getWidth(), resizeDto.getHeight());
        } catch (Exception e) {
            this.sendCommand(handlerItem, message);
        }
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

    private void sendCommand(HandlerItem handlerItem, String data) throws Exception {
        handlerItem.outputStream.write(data.getBytes());
        handlerItem.outputStream.flush();
    }

    public class HandlerItem implements Runnable {
        private final Long userId;
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

            this.openSession = shellSession;

            CommandLogDto commandLogDto = SpringUtil.getBean(CommandLogService.class)
                                                    .create(new CommandLogCreateParams(
                                                            userId,
                                                            session.getId(),
                                                            serverId
                                                    ));
            log.info("创建命令记录成功：{}", commandLogDto.getId());
            logFile = FileUtil.file(commandLogDto.getCommandData());
            //5mb的日志缓冲区
            logFileOutputStream = IoUtil.toBuffered(
                    FileUtil.getOutputStream(logFile),
                    1024 * 1024 * 5
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
        public void addSubSession(javax.websocket.Session session) {
            this.sessions.add(session);
            //写入之前的数据
            logFileOutputStream.flush();

            sendBinary(session, FileUtil.readString(logFile, openSession.getRemoteCharset()));
        }

        @SneakyThrows
        public void removeSubSession(javax.websocket.Session session) {
            this.sessions.remove(session);
            IoUtil.close(session);
        }

        public Long getServerId() {
            return serverId;
        }

        public Long getUserId() {
            return userId;
        }

        @Override
        public void run() {
            log.info("开始读取数据");
            try {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != -1) {
                    String s = new String(Arrays.copyOfRange(buffer, 0, i), openSession.getRemoteCharset());
                    logFileOutputStream.write(s.getBytes());
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

    @SneakyThrows
    public void destroy(javax.websocket.Session session) {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());
        if (handlerItem != null) {
            handlerItem.close();
        }

        IoUtil.close(session);
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
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
}
