package com.codeages.termiusplus.biz.patrol.agent;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 工具调用事件，用于向前端推送工具执行过程
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToolCallEvent {
    private String type;    // "tool_start" | "tool_result"
    private String toolName;
    private String arguments;
    private String result;
    private long durationMs;
}
