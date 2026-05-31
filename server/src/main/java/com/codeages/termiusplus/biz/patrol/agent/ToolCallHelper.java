package com.codeages.termiusplus.biz.patrol.agent;

import reactor.core.publisher.Sinks;

/**
 * 工具调用辅助类，用于在工具执行时上报事件
 */
public class ToolCallHelper {

    /**
     * 包装工具执行，自动上报开始和结束事件（使用当前 sink）
     */
    public static <T> T execute(String toolName, String arguments, ToolExecutor<T> executor) {
        return execute(null, toolName, arguments, executor);
    }

    /**
     * 包装工具执行，自动上报开始和结束事件（指定 sink）
     */
    public static <T> T execute(Sinks.Many<String> sink, String toolName, String arguments, ToolExecutor<T> executor) {
        long startTime = System.currentTimeMillis();
        ToolCallEventCollector.emitToolStart(sink, toolName, arguments);
        try {
            T result = executor.execute();
            long duration = System.currentTimeMillis() - startTime;
            String resultStr = result != null ? result.toString() : "";
            ToolCallEventCollector.emitToolResult(sink, toolName, arguments, resultStr, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            ToolCallEventCollector.emitToolResult(sink, toolName, arguments, "执行失败: " + e.getMessage(), duration);
            throw e;
        }
    }

    @FunctionalInterface
    public interface ToolExecutor<T> {
        T execute();
    }
}
