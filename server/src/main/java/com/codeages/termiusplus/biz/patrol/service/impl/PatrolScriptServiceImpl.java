package com.codeages.termiusplus.biz.patrol.service.impl;

import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptCreateParams;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptDto;
import com.codeages.termiusplus.biz.patrol.dto.PatrolScriptUpdateParams;
import com.codeages.termiusplus.biz.patrol.entity.PatrolScript;
import com.codeages.termiusplus.biz.patrol.mapper.PatrolScriptMapper;
import com.codeages.termiusplus.biz.patrol.repository.PatrolScriptRepository;
import com.codeages.termiusplus.biz.patrol.service.PatrolScriptService;
import com.codeages.termiusplus.exception.AppException;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.spring.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PatrolScriptServiceImpl implements PatrolScriptService {

    private final PatrolScriptRepository repository;
    private final PatrolScriptMapper mapper;
    private final ScriptGeneratorAiService scriptGeneratorAiService;

    @Override
    public PatrolScriptDto create(PatrolScriptCreateParams params) {
        PatrolScript entity = mapper.toCreateEntity(params);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    public PatrolScriptDto update(PatrolScriptUpdateParams params) {
        PatrolScript entity = repository.findById(params.getId())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        mapper.toUpdateEntity(entity, params);
        return mapper.toDto(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(Long id) {
        repository.deleteById(id);
    }

    @Override
    public PatrolScriptDto findById(Long id) {
        return repository.findById(id)
                .map(mapper::toDto)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
    }

    @Override
    public List<PatrolScriptDto> findAll() {
        return repository.findAll().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<PatrolScriptDto> findEnabled() {
        return repository.findAllByEnabledTrue().stream()
                .map(mapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public PatrolScriptDto generateScript(String description) {
        String result = scriptGeneratorAiService.generate(description);
        PatrolScriptCreateParams params = parseAiResult(result, description);

        PatrolScriptDto dto = new PatrolScriptDto();
        dto.setName(params.getName());
        dto.setDescription(params.getDescription());
        dto.setScriptContent(params.getScriptContent());
        dto.setOutputSchema(params.getOutputSchema());
        dto.setCategory(params.getCategory());
        return dto;
    }

    private PatrolScriptCreateParams parseAiResult(String aiResult, String originalDescription) {
        PatrolScriptCreateParams params = new PatrolScriptCreateParams();
        String[] sections = aiResult.split("---");
        for (int i = 0; i < sections.length; i++) {
            String section = sections[i].trim();
            switch (section) {
                case "NAME":
                    if (i + 1 < sections.length) params.setName(sections[++i].trim());
                    break;
                case "DESCRIPTION":
                    if (i + 1 < sections.length) params.setDescription(sections[++i].trim());
                    break;
                case "SCRIPT":
                    if (i + 1 < sections.length) params.setScriptContent(sections[++i].trim());
                    break;
                case "SCHEMA":
                    if (i + 1 < sections.length) params.setOutputSchema(sections[++i].trim());
                    break;
                case "CATEGORY":
                    if (i + 1 < sections.length) params.setCategory(sections[++i].trim());
                    break;
            }
        }
        if (params.getName() == null) {
            params.setName(originalDescription.substring(0, Math.min(50, originalDescription.length())));
        }
        if (params.getCategory() == null) params.setCategory("custom");
        return params;
    }

    @AiService
    interface ScriptGeneratorAiService {
        @SystemMessage("你是一个 Linux 服务器运维专家。根据用户描述的巡查需求，生成一个 shell 脚本。" +
                "要求：\n" +
                "1. 脚本必须输出 JSON 格式结果到 stdout\n" +
                "2. JSON 必须包含字段：status(ok/warning/error), message(简要描述), details(详细数据)\n" +
                "3. 脚本应该是可独立执行的 bash 脚本\n" +
                "4. 不要使用交互式命令\n" +
                "5. 按以下格式返回，每个部分用 --- 标记分隔：\n" +
                "---NAME---\n脚本名称\n" +
                "---DESCRIPTION---\n脚本描述\n" +
                "---SCRIPT---\n#!/bin/bash\n...\n" +
                "---SCHEMA---\nJSON Schema 描述输出格式\n" +
                "---CATEGORY---\ndisk/nginx/security/service/custom")
        String generate(String userMessage);
    }
}
