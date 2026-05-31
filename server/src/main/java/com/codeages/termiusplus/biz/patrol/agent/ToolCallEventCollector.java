package com.codeages.termiusplus.biz.patrol.agent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.concurrent.ConcurrentHashMap;

/**
 * 工具调用事件收集器
 * 使用 conversationId 作为 key 存储 sink，解决 WebFlux 多线程问题
 */
public class ToolCallEventCollector {

    private static final ConcurrentHashMap<String, Sinks.Many<ToolCallEvent>> SINKS = new ConcurrentHashMap<>();
    private static volatile String currentConversationId;

    /**
     * 为当前请求创建新的事件 sink
     */
    public static Sinks.Many<ToolCallEvent> createSink(String conversationId) {
        Sinks.Many<ToolCallEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        SINKS.put(conversationId, sink);
        currentConversationId = conversationId;
        return sink;
    }

    /**
     * 获取当前请求的事件 sink
     */
    public static Sinks.Many<ToolCallEvent> getCurrentSink() {
        String convId = currentConversationId;
        if (convId != null) {
            return SINKS.get(convId);
        }
        return null;
    }

    /**
     * 清理指定会话的事件 sink
     */
    public static void clear(String conversationId) {
        SINKS.remove(conversationId);
        if (conversationId != null && conversationId.equals(currentConversationId)) {
            currentConversationId = null;
        }
    }

    /**
     * 工具开始执行时调用
     */
    public static void emitToolStart(String toolName, String arguments) {
        Sinks.Many<ToolCallEvent> sink = getCurrentSink();
        if (sink != null) {
            sink.tryEmitNext(new ToolCallEvent("tool_start", toolName, arguments, null, 0));
        }
    }

    /**
     * 工具执行完成时调用
     */
    public static void emitToolResult(String toolName, String arguments, String result, long durationMs) {
        Sinks.Many<ToolCallEvent> sink = getCurrentSink();
        if (sink != null) {
            sink.tryEmitNext(new ToolCallEvent("tool_result", toolName, arguments, result, durationMs));
        }
    }
}
