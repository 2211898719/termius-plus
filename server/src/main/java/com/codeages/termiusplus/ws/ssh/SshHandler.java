package com.codeages.termiusplus.ws.ssh;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.lang.Pair;
import cn.hutool.core.text.CharSequenceUtil;
import cn.hutool.core.thread.ExecutorBuilder;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.log.dto.CommandLogCreateParams;
import com.codeages.termiusplus.biz.log.dto.CommandLogDto;
import com.codeages.termiusplus.biz.log.service.CommandLogService;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.user.dto.UserDto;
import com.codeages.termiusplus.biz.user.service.UserService;
import com.codeages.termiusplus.exception.AppException;
import com.codeages.termiusplus.security.AuthTokenFilter;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.UserAuthException;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Component;

import java.io.*;
import java.net.ConnectException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static com.codeages.termiusplus.biz.server.context.ServerContext.SSH_POOL;

/**
 * sessionId用来标识客户端，用于拉起键盘验证授权识别客户端使用
 * serverId用来标识连接的服务器
 * masterSessionId用来标识是否为主连接，如果是0，说明是主连接
 */
@Slf4j
@Component
@ServerEndpoint("/socket/ssh/{sessionId}/{serverId}/{masterSessionId}")
public class SshHandler {

    private static final int MAX_LOG_BUFFER_SIZE = 1024 * 1024 * 1;

    private static final String NONE_MASTER_SESSION_ID = "0";

    private static final ThreadPoolExecutor threadPoolExecutor = ExecutorBuilder.create()
                                                                                .setThreadFactory(ThreadUtil.createThreadFactory("ssh-handler-thread-"))
                                                                                .setAllowCoreThreadTimeOut(false)
                                                                                .useSynchronousQueue()
                                                                                .build();


    private final ServerService serverService;

    private final CommandLogService commandLogService;

    private final UserService userService;

    public SshHandler() {
        this.serverService = SpringUtil.getBean(ServerService.class);
        this.commandLogService = SpringUtil.getBean(CommandLogService.class);
        this.userService = SpringUtil.getBean(UserService.class);
    }

    @OnOpen
    public void onOpen(Session session, @PathParam("sessionId") String sessionId,
                       @PathParam("serverId") Long serverId, @PathParam("masterSessionId") String masterSessionId) {
        log.info("ssh onOpen");
        Long userId = AuthTokenFilter.userIdThreadLocal.get();
        if (userId == null) {
            log.error("用户未登录");
            throw new AppException(ErrorCode.INTERNAL_ERROR, "用户鉴权不通过");
        }
        // 为了防止内存泄漏，这里需要手动清理
        AuthTokenFilter.userIdThreadLocal.remove();

        String username = userService.get(userId)
                                     .map(UserDto::getUsername)
                                     .orElse("");

        log.info("有连接加入,sessionId={}", session.getId());

        // 如果是主连接 0，就创建一个新的连接
        if (isMasterSession(masterSessionId)) {
            HandlerItem handlerItem = new HandlerItem(
                    userId,
                    username,
                    serverId,
                    session,
                    serverService.createSSHClient(serverId, sessionId)
            );

            log.info("创建连接成功：{}", session.getId());
            SSH_POOL.put(session.getId(), handlerItem);
            String message = JSONUtil.toJsonStr(new MessageDto(EventType.SESSION, JSONUtil.toJsonStr(session.getId())));
            handlerItem.sendMasterBinary(message);
        } else {
            // 如果不是0，说明是子连接，找到主连接，然后加入到主连接的sessions中
            HandlerItem handlerItem = SSH_POOL.get(masterSessionId);
            if (handlerItem == null) {
                log.error("主连接不存在：{}", masterSessionId);
                sendBinary(session, JSONUtil.toJsonStr(new MessageDto(EventType.MASTER_CLOSE, "主连接不存在或已关闭")));
                return;
            }

            handlerItem.addSubSession(session, username);
        }
    }


    @OnMessage
    public void onMessage(@PathParam("masterSessionId") String masterSessionId, byte[] message,
                          Session session) {
        HandlerItem handlerItem = SSH_POOL.get(session.getId());
        if (!isMasterSession(masterSessionId)) {
            handlerItem = SSH_POOL.get(masterSessionId);
        }

        if (handlerItem == null) {
            log.error("主连接不存在：{}", session.getId());
            return;
        }

        handlerItem.active();
        //如果是事件，就不发送到服务器
        MessageDto messageDto = MessageDto.parse(decompressString(message, Charset.defaultCharset()));
        switch (messageDto.getEvent()) {
            case COMMAND:
                sendCommand(handlerItem, messageDto.getData());
                break;
            case RESIZE:
                //只有主连接才能调整窗口大小
                if (isMasterSession(masterSessionId)) {
                    ResizeDto resizeDto = JSONUtil.toBean(messageDto.getData(), ResizeDto.class);
                    handlerItem.reSize(
                            resizeDto.getCols(),
                            resizeDto.getRows(),
                            resizeDto.getWidth(),
                            resizeDto.getHeight()
                                      );
                }
                break;
            case REQUEST_AUTH_EDIT_SESSION:
                handlerItem.reqAuthEditSession(session);
                break;
            case RESPONSE_AUTH_EDIT_SESSION:
                handlerItem.handleResAuthEditSession(session, messageDto);
                break;
            default:
                log.error("未知事件：{}", messageDto.getEvent());
                break;
        }
    }

    @OnClose
    public void onClose(Session session, @PathParam("masterSessionId") String masterSessionId) {
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
    public void onError(Session session, Throwable error,
                        @PathParam("masterSessionId") String masterSessionId) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());

        String message = "发生错误：" + error.getMessage();
        if (error instanceof UserAuthException) {
            message = "用户认证错误";
        } else if (error instanceof ConnectException) {
            message = "网络连接错误，检查ip和端口是否正确";
        }

        MessageDto messageDto = new MessageDto(EventType.COMMAND, message);
        String s = JSONUtil.toJsonStr(messageDto);

        sendBinary(session, s);

        error.printStackTrace();

        if (isMasterSession(masterSessionId)) {
            HandlerItem handlerItem = SSH_POOL.get(session.getId());
            if (handlerItem != null) {
                handlerItem.close();
            }
            SSH_POOL.remove(session.getId());
        } else {
            HandlerItem handlerItem = SSH_POOL.get(masterSessionId);
            if (handlerItem != null) {
                handlerItem.removeSubSession(session);
            }
        }
    }

    private void sendCommand(HandlerItem handlerItem, String data) {
        try {
            handlerItem.outputStream.write(data.getBytes());
            handlerItem.outputStream.flush();
        } catch (IOException e) {
            log.error("发送命令失败：{}", e.getMessage());
        }

    }

    @SneakyThrows
    private static byte[] compressString(String input, Charset charset) {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        GZIPOutputStream gzipOut = new GZIPOutputStream(bytes);
        gzipOut.write(input.getBytes(charset));
        gzipOut.close();
        return bytes.toByteArray();
    }

    @SneakyThrows
    private static String decompressString(byte[] input, Charset charset) {
        GZIPInputStream gzipIn = new GZIPInputStream(new ByteArrayInputStream(input));
        return IoUtil.read(gzipIn, charset);
    }

    private synchronized void sendBinary(Session session, String msg) {
        if (!session.isOpen()) {
            // 会话关闭不能发送消息
            return;
        }

        try {
            session.getBasicRemote()
                   .sendBinary(ByteBuffer.wrap(compressString(msg, Charset.defaultCharset())));
        } catch (Exception e) {
            log.error("发送消息出错", e);
        }
    }

    private boolean isMasterSession(String masterSessionId) {
        return NONE_MASTER_SESSION_ID.equals(masterSessionId);
    }

    @SneakyThrows
    public void destroy(Session session) {
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
        private final Session masterSession;
        private final List<Pair<String, Session>> sessions = new CopyOnWriteArrayList<>();
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final SSHClient sshClient;
        private final net.schmizz.sshj.connection.channel.direct.Session openSession;
        private final net.schmizz.sshj.connection.channel.direct.Session.Shell shell;
        @Getter
        private long lastActiveTime = System.currentTimeMillis();
        private long initTime = System.currentTimeMillis();
        private final File logFile;
        private final BufferedOutputStream logFileOutputStream;
        private final RollingString lastCommand;
        private final ServerDto serverDto;

        @SneakyThrows
        HandlerItem(Long userId, String username, Long serverId, Session session, SSHClient sshClient) {
            this.sessions.add(Pair.of(username, session));
            this.userId = userId;
            this.serverId = serverId;
            this.sshClient = sshClient;

            this.masterSessionId = session.getId();
            this.masterSession = session;
            net.schmizz.sshj.connection.channel.direct.Session shellSession = sshClient.startSession();
            shellSession.allocatePTY("xterm", 80, 24, 640, 480, Map.of());
            shellSession.setAutoExpand(true);

            this.shell = shellSession.startShell();

            this.inputStream = shell.getInputStream();
            this.outputStream = shell.getOutputStream();

            //auto sudo
            serverDto = serverService.findById(serverId);
            if (Boolean.TRUE.equals(serverDto.getAutoSudo()) && ObjectUtil.notEqual(serverDto.getUsername(), "root")) {
                outputStream.write((buildSudoCommand() + "\n").getBytes());
                outputStream.flush();
            }

            if (CharSequenceUtil.isNotBlank(serverDto.getFirstCommand())) {
                outputStream.write((serverDto.getFirstCommand()).getBytes());
                outputStream.flush();
            }

            this.openSession = shellSession;

            CommandLogDto commandLogDto = commandLogService.create(new CommandLogCreateParams(
                    userId,
                    session.getId(),
                    serverId
            ));
            log.info("创建命令记录成功：{}", commandLogDto.getId());
            logFile = FileUtil.file(commandLogDto.getCommandData());

            //日志内存缓冲区
            logFileOutputStream = IoUtil.toBuffered(FileUtil.getOutputStream(logFile), MAX_LOG_BUFFER_SIZE);

            lastCommand = new RollingString();
            threadPoolExecutor.submit(this);
        }

        private @NotNull String buildSudoCommand() {
            return " echo '" + serverDto.getPassword() + "' | sudo -S echo '' && sudo -i";
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
            } catch (Exception ignored) {

            }
            try {
                IoUtil.close(this.outputStream);
            } catch (Exception ignored) {

            }

            try {
                IoUtil.close(this.logFileOutputStream);
            } catch (Exception ignored) {

            }

            try {
                IoUtil.close(sshClient);
            } catch (Exception ignored) {

            }

            try {
                String message = JSONUtil.toJsonStr(new MessageDto(EventType.MASTER_CLOSE, "主连接已关闭"));

                sessions.stream()
                        .map(Pair::getValue)
                        .filter(s -> s.isOpen() && !s.getId()
                                                     .equals(masterSessionId))
                        .forEach(s -> sendBinary(s, message));
            } catch (Exception ignored) {

            }

            try {
                this.shell.close();
            } catch (Exception ignored) {

            }

            try {
                this.openSession.close();
            } catch (Exception ignored) {

            }

            try {
                sessions.stream()
                        .map(Pair::getValue)
                        .filter(s -> s.isOpen() && !s.getId()
                                                     .equals(masterSessionId))
                        .forEach(IoUtil::close);
            } catch (Exception ignored) {

            }

            //停掉当前线程
            threadPoolExecutor.remove(this);
        }

        @SneakyThrows
        public void addSubSession(Session session, String username) {
            this.sessions.add(Pair.of(username, session));

            //写入内存缓冲区中的数据到文件
            logFileOutputStream.flush();

            MessageDto messageDto = new MessageDto(
                    EventType.COMMAND,
                    FileUtil.readString(logFile, openSession.getRemoteCharset())
            );

            //把之前的服务器上下文的数据发送给新的子连接
            sendBinary(session, JSONUtil.toJsonStr(messageDto));

            //发送一个JOIN_SESSION事件，用于前端标识
            MessageDto joinSessionMessageDto = new MessageDto(EventType.JOIN_SESSION, username);

            sendMasterBinary(JSONUtil.toJsonStr(joinSessionMessageDto));
        }

        @SneakyThrows
        public void removeSubSession(Session session) {
            log.info("有连接关闭,session:{}", sessions.size());
            String username = null;
            for (int i = 0; i < sessions.size(); i++) {
                Pair<String, Session> currentSession = sessions.get(i);
                if (currentSession.getValue() == session) {
                    username = currentSession.getKey();
                    sessions.remove(i);
                    break;
                }
            }

            log.info("有连接关闭,session:{}", sessions.size());
            IoUtil.close(session);

            MessageDto joinSessionMessageDto = new MessageDto(EventType.LEAVE_SESSION, username);
            sendMasterBinary(JSONUtil.toJsonStr(joinSessionMessageDto));
        }

        private void sendMasterBinary(String msg) {
            for (Pair<String, Session> session : sessions) {
                if (session.getValue()
                           .getId()
                           .equals(masterSessionId)) {
                    sendBinary(session.getValue(), msg);
                    break;
                }
            }
        }

        @Override
        public void run() {
            try {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != -1) {
                    String originData = new String(Arrays.copyOfRange(buffer, 0, i), openSession.getRemoteCharset());
                    if (System.currentTimeMillis() - initTime < 6000 && Boolean.TRUE.equals(serverDto.getAutoSudo()) && ObjectUtil.notEqual(
                            serverDto.getUsername(),
                            "root"
                                                                                                                                            ) && originData.contains(buildSudoCommand())) {
                        originData = originData.replace(buildSudoCommand(), "");
                    }

                    MessageDto messageDto = new MessageDto(EventType.COMMAND, originData);
                    String s = JSONUtil.toJsonStr(messageDto);
                    logFileOutputStream.write(originData.getBytes());
                    lastCommand.append(originData);
                    sessions.forEach(session -> sendBinary(session.getValue(), s));
                }
            } catch (Exception e) {
                if (!this.openSession.isOpen()) {
                    return;
                }

                log.error("读取数据失败", e);
                sessions.stream()
                        .map(Pair::getValue)
                        .forEach(SshHandler.this::destroy);
            }
        }

        public void active() {
            lastActiveTime = System.currentTimeMillis();
        }

        public String getLastCommand() {
            return lastCommand.toString();
        }

        public void reqAuthEditSession(Session session) {
            String username = sessions.stream()
                                      .filter(s -> s.getValue()
                                                    .getId()
                                                    .equals(session.getId()))
                                      .map(Pair::getKey)
                                      .findFirst()
                                      .orElse("");

            AuthEditSessionDto authEditSessionDto = new AuthEditSessionDto(username, session.getId(), null);

            String message = JSONUtil.toJsonStr(new MessageDto(
                    EventType.REQUEST_AUTH_EDIT_SESSION,
                    JSONUtil.toJsonStr(authEditSessionDto)
            ));
            sendMasterBinary(message);
        }

        public boolean isOpen() {
            return openSession.isOpen() && shell.isOpen() && masterSession.isOpen();
        }

        public void handleResAuthEditSession(Session session, MessageDto messageDto) {
            AuthEditSessionDto authEditSessionDto = JSONUtil.toBean(messageDto.getData(), AuthEditSessionDto.class);
            sessions.stream()
                    .filter(s -> s.getValue()
                                  .getId()
                                  .equals(authEditSessionDto.getSessionId()))
                    .map(Pair::getValue)
                    .findFirst()
                    .ifPresent(subSession -> sendBinary(subSession, JSONUtil.toJsonStr(messageDto)));
        }
    }
}
