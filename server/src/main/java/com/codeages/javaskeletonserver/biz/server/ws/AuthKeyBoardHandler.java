package com.codeages.javaskeletonserver.biz.server.ws;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.thread.ThreadUtil;
import cn.hutool.extra.spring.SpringUtil;
import cn.hutool.json.JSONUtil;
import com.codeages.javaskeletonserver.biz.server.context.ServerContext;
import com.codeages.javaskeletonserver.biz.server.service.ServerService;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import net.schmizz.sshj.SSHClient;
import net.schmizz.sshj.userauth.UserAuthException;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/socket/authKeyBoard")
public class AuthKeyBoardHandler {

    private static final ConcurrentHashMap<String, AuthKeyBoardHandler> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private static final AtomicInteger OnlineCount = new AtomicInteger(0);

    @OnOpen
    public void onOpen(Session session) {
        int cnt = OnlineCount.incrementAndGet();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), this);
        log.info("有连接加入，当前连接数为：{},sessionId={}", cnt, session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        int cnt = OnlineCount.decrementAndGet();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        log.info("有连接关闭，当前连接数为：{}", cnt);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        // root@192.168.1.1 123456
        AuthKeyBoardOnMessageDto authKeyBoardOnMessageDto = JSONUtil.toBean(message, AuthKeyBoardOnMessageDto.class);
        ServerContext.AUTH_KEYBOARD_INTERACTIVE_POOL.get(authKeyBoardOnMessageDto.getServerKey()).offer(authKeyBoardOnMessageDto.getPassword());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());

        error.printStackTrace();
    }

    public static void SendMessage(javax.websocket.Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }

}
