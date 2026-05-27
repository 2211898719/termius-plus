package com.codeages.termiusplus.biz.patrol.service;

import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptCreateParams;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptDto;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptUpdateParams;

import java.util.List;

public interface PatrolScriptService {
    PatrolScriptDto create(PatrolScriptCreateParams params);
    PatrolScriptDto update(PatrolScriptUpdateParams params);
    void delete(Long id);
    PatrolScriptDto findById(Long id);
    List<PatrolScriptDto> findAll();
    List<PatrolScriptDto> findEnabled();
    PatrolScriptDto generateScript(String description);
}
