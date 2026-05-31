package com.codeages.termiusplus.biz.patrol.agent;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

/**
 * 工具调用事件收集器
 * 工具在执行时通过此类上报事件，流式响应订阅此事件流
 */
public class ToolCallEventCollector {

    private static final ThreadLocal<Sinks.Many<ToolCallEvent>> CURRENT_SINK = new ThreadLocal<>();

    /**
     * 为当前请求创建新的事件 sink
     */
    public static Sinks.Many<ToolCallEvent> createSink() {
        Sinks.Many<ToolCallEvent> sink = Sinks.many().multicast().onBackpressureBuffer();
        CURRENT_SINK.set(sink);
        return sink;
    }

    /**
     * 获取当前请求的事件 sink
     */
    public static Sinks.Many<ToolCallEvent> getCurrentSink() {
        return CURRENT_SINK.get();
    }

    /**
     * 清理当前请求的事件 sink
     */
    public static void clear() {
        CURRENT_SINK.remove();
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

    /**
     * 订阅事件流
     */
    public static Flux<ToolCallEvent> getEventFlux() {
        Sinks.Many<ToolCallEvent> sink = getCurrentSink();
        if (sink != null) {
            return sink.asFlux();
        }
        return Flux.empty();
    }
}
