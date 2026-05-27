package com.codeages.termiusplus.biz.patrol.service;

import com.codeages.termiusplus.biz.patrol.dto.PatrolTaskDto;

import java.util.List;

public interface PatrolEngine {
    PatrolTaskDto executeScript(Long scriptId, Long serverId);
    List<PatrolTaskDto> executeAll();
    List<PatrolTaskDto> executeScriptOnAllServers(Long scriptId);
}
