package com.codeages.termiusplus.biz.patrol.agent;

/**
 * 工具调用辅助类，用于在工具执行时上报事件
 */
public class ToolCallHelper {

    /**
     * 包装工具执行，自动上报开始和结束事件
     */
    public static <T> T execute(String toolName, String arguments, ToolExecutor<T> executor) {
        long startTime = System.currentTimeMillis();
        ToolCallEventCollector.emitToolStart(toolName, arguments);
        try {
            T result = executor.execute();
            long duration = System.currentTimeMillis() - startTime;
            String resultStr = result != null ? result.toString() : "";
            // 截断过长的结果
            if (resultStr.length() > 2000) {
                resultStr = resultStr.substring(0, 2000) + "\n... (结果已截断)";
            }
            ToolCallEventCollector.emitToolResult(toolName, arguments, resultStr, duration);
            return result;
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            ToolCallEventCollector.emitToolResult(toolName, arguments, "执行失败: " + e.getMessage(), duration);
            throw e;
        }
    }

    @FunctionalInterface
    public interface ToolExecutor<T> {
        T execute();
    }
}
