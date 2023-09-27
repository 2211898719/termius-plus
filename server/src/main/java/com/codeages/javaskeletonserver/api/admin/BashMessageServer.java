package com.codeages.javaskeletonserver.api.admin;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import javax.websocket.*;
import javax.websocket.server.PathParam;
import javax.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;


@Slf4j
@Component
@ServerEndpoint("/api-admin/user/{userId}")
public class BashMessageServer {

    /**
     * concurrent包的线程安全Set，用来存放每个客户端对应的WebSocket对象。
     */
    private static final ConcurrentHashMap<String, List<BashMessageServer>> webSocketMap = new ConcurrentHashMap<>();
    /**
     * 与某个客户端的连接会话，需要通过它来给客户端发送数据
     */
    private Session session;
    /**
     * 接收userId
     */
    private String userId = "";

    /**
     * 连接建立成
     * 功调用的方法
     */
    @OnOpen
    public void onOpen(Session session, @PathParam("userId") String userId) {
        this.session = session;
        this.userId = userId;
        if (webSocketMap.containsKey(userId)) {
            List<BashMessageServer> servers = webSocketMap.getOrDefault(userId, new ArrayList<>());
            //加入set中
            servers.add(this);
            webSocketMap.put(userId, servers);
        } else {
            //加入set中
            List<BashMessageServer> servers = new ArrayList<>();
            servers.add(this);
            webSocketMap.put(userId, servers);
        }
        log.info("用户连接:" + userId + ",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 连接关闭
     * 调用的方法
     */
    @OnClose
    public void onClose() {
        if (webSocketMap.containsKey(userId)) {
            webSocketMap.get(userId).removeIf(s -> s.session.getId().equals(this.session.getId()));
        }
        log.info("用户退出:" + userId + ",当前在线人数为:" + getOnlineCount());
    }

    /**
     * 收到客户端消
     * 息后调用的方法
     *
     * @param message 客户端发送过来的消息
     **/
    @OnMessage
    public void onMessage(String message, Session session) {
        if (StrUtil.isBlank(message)) {
            return;
        }

        if ("ping".equals(message)) {
            return;
        }

        log.info("用户消息:" + userId + ",报文:" + message);

        webSocketMap.get(this.userId).forEach(s -> s.sendMessage(message));
    }


    /**
     * @param session
     * @param error
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("用户错误:" + this.userId + ",原因:" + error.getMessage());
        error.printStackTrace();
    }

    /**
     * 实现服务
     * 器主动推送
     */
    public void sendMessage(String message) {
        synchronized (this.session) {
            if (this.session == null) {
                return;
            }

            if (!this.session.isOpen()) {
                return;
            }
            try {
                this.session.getBasicRemote().sendText(message);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 发送自定
     * 义消息
     **/
    public static void sendInfo(Long messageId, String message, String userId) {
        if (StringUtils.isBlank(userId) || !webSocketMap.containsKey(userId)) {
            log.error("用户" + userId + ",不在线！");
            return;
        }

        String json = "";

        log.info("发送消息到:" + userId + "，报文:" + json);
        webSocketMap.get(userId).forEach(s -> s.sendMessage(json));
    }

    /**
     * 获得此时的
     * 在线人数
     *
     * @return
     */
    public static synchronized int getOnlineCount() {
        return webSocketMap.size();
    }

}


