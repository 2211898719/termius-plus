package com.codeages.termiusplus.biz.patrol.service;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;

import java.util.List;

public interface PatrolEngine {
    PatrolTaskDto executeScript(Long scriptId, Long serverId);
    List<PatrolTaskDto> executeAll();
    List<PatrolTaskDto> executeScriptOnAllServers(Long scriptId);
    /**
     * 在指定服务器上跑临时脚本(测试用),不写入 patrol_task 表,仅返回结果 DTO。
     */
    PatrolTaskDto executeDraft(String scriptContent, String scriptName, Long serverId);
}
