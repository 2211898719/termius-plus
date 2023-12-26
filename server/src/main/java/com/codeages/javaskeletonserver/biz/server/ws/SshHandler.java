package com.codeages.javaskeletonserver.biz.server.ws;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/socket/ssh/{serverId}")
public class SshHandler {

    private static final ConcurrentHashMap<String, HandlerItem> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private static final AtomicInteger OnlineCount = new AtomicInteger(0);

    @OnOpen
    public void onOpen(javax.websocket.Session session, @PathParam("serverId") Long serverId) {
        int cnt = OnlineCount.incrementAndGet();
        log.info("有连接加入，当前连接数为：{},sessionId={}", cnt, session.getId());
        HandlerItem handlerItem = new HandlerItem(
                serverId,
                session,
                SpringUtil.getBean(ServerService.class).createSSHClient(serverId)
        );
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), handlerItem);
    }

    @OnClose
    public void onClose(javax.websocket.Session session) {
        int cnt = OnlineCount.decrementAndGet();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId()).close();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        log.info("有连接关闭，当前连接数为：{}", cnt);
    }

    @OnMessage
    public void onMessage(String message, javax.websocket.Session session) throws Exception {
        HandlerItem handlerItem = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(session.getId());

        try {
            MessageDto messageDto = MessageDto.parse(message);
            ResizeDto resizeDto = JSONUtil.toBean(messageDto.getData(), ResizeDto.class);
            handlerItem.reSize(resizeDto.getCols(), resizeDto.getRows(), resizeDto.getWidth(), resizeDto.getHeight());
            return;
        } catch (Exception e) {
            this.sendCommand(handlerItem, message);
        }
    }

    @OnError
    public void onError(javax.websocket.Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());
        error.printStackTrace();
    }

    private void sendCommand(HandlerItem handlerItem, String data) throws Exception {
        if (handlerItem.checkInput(data)) {
            handlerItem.outputStream.write(data.getBytes());
            handlerItem.append(data);
        } else {
            handlerItem.outputStream.write("没有执行相关命令权限".getBytes());
            handlerItem.outputStream.flush();
            handlerItem.outputStream.write(new byte[]{3});
        }
        handlerItem.outputStream.flush();
    }

    public static void SendMessage(javax.websocket.Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    private class HandlerItem implements Runnable {
        private final javax.websocket.Session session;
        private final Long serverId;
        private final InputStream inputStream;
        private final OutputStream outputStream;
        private final net.schmizz.sshj.connection.channel.direct.Session openSession;
        private final net.schmizz.sshj.connection.channel.direct.Session.Shell shell;

        @SneakyThrows
        HandlerItem(Long serverId, javax.websocket.Session session, SSHClient sshClient) {
            this.session = session;
            this.serverId = serverId;

            sshClient.useCompression();
            net.schmizz.sshj.connection.channel.direct.Session shellSession = sshClient.startSession();
            shellSession.allocatePTY("xterm", 80, 24, 640, 480, Map.of());
            shellSession.setAutoExpand(true);
            this.shell = shellSession.startShell();

            this.inputStream = shell.getInputStream();
            this.outputStream = shell.getOutputStream();

            this.openSession = shellSession;
            ThreadUtil.execute(this);
        }

        /**
         * 添加到命令队列
         *
         * @param msg 输入
         * @return 当前待确认待所有命令
         */
        private void append(String msg) {
//            commandMap.put(System.currentTimeMillis(), msg);
        }

        public boolean checkInput(String msg) {
            return true;
        }

        public void reSize(int col, int row, int width, int height) {
            try {
                shell.changeWindowDimensions(col, row, width, height);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        public void close() {
            try {
                IoUtil.close(this.inputStream);
                IoUtil.close(this.outputStream);
                this.shell.close();
                this.openSession.close();
//                SpringUtil.getBean(CommandLogService.class).create(new CommandLogCreateParams(
//                        this.session.getId(),
//                        this.serverId,
//                        JSONUtil.toJsonStr(commandMap)
//                ));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void run() {
            log.info("开始读取数据");
            try {
                byte[] buffer = new byte[1024];
                int i;
                //如果没有数据来，线程会一直阻塞在这个地方等待数据。
                while ((i = inputStream.read(buffer)) != -1) {
                    sendBinary(session, new String(Arrays.copyOfRange(buffer, 0, i), openSession.getRemoteCharset()));
                }
            } catch (Exception e) {
                if (!this.openSession.isOpen()) {
                    return;
                }

                SshHandler.this.destroy(this.session);
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
