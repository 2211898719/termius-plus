package com.codeages.termiusplus.ws.ssh;

import cn.hutool.json.JSONUtil;
import com.codeages.termiusplus.biz.server.context.ServerContext;
import com.codeages.termiusplus.security.AuthTokenFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import jakarta.websocket.*;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Component
@ServerEndpoint("/socket/authKeyBoard")
public class AuthKeyBoardHandler {

    private static final ConcurrentHashMap<String, Session> HANDLER_ITEM_CONCURRENT_HASH_MAP = new ConcurrentHashMap<>();

    private static final AtomicInteger OnlineCount = new AtomicInteger(0);

    @OnOpen
    public void onOpen(Session session) {
        AuthTokenFilter.userIdThreadLocal.remove();
        int cnt = OnlineCount.incrementAndGet();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.put(session.getId(), session);
        log.info("有连接加入，当前连接数为：{},sessionId={}", cnt, session.getId());
        MessageDto messageDto = new MessageDto(EventType.SESSION, session.getId());
        sendMessage(session, JSONUtil.toJsonStr(messageDto));
    }

    @OnClose
    public void onClose(Session session) {
        int cnt = OnlineCount.decrementAndGet();
        HANDLER_ITEM_CONCURRENT_HASH_MAP.remove(session.getId());
        log.info("有连接关闭，当前连接数为：{}", cnt);
    }

    @OnMessage
    public void onMessage(String message, Session session) {
        AuthKeyBoardDto authKeyBoardOnMessageDto = JSONUtil.toBean(message, AuthKeyBoardDto.class);
        ServerContext.AUTH_KEYBOARD_INTERACTIVE_POOL.get(authKeyBoardOnMessageDto.getServerKey())
                                                    .offer(authKeyBoardOnMessageDto.getPassword());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("发生错误：{}，Session ID： {}", error.getMessage(), session.getId());

        error.printStackTrace();
    }

    public static void sendMessage(Session session, String message) {
        try {
            session.getBasicRemote().sendText(message);
        } catch (IOException e) {
            log.error("发送消息出错：{}", e.getMessage());
            e.printStackTrace();
        }
    }

    public static void sendMessage(String sessionId, String message) {
        Session session = HANDLER_ITEM_CONCURRENT_HASH_MAP.get(sessionId);
        if (session != null) {
            sendMessage(session, message);
        }
    }

}
