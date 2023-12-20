package com.codeages.javaskeletonserver.biz.log.service.impl;

import cn.hutool.core.util.StrUtil;

import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.log.entity.QCommandLog;
import com.codeages.javaskeletonserver.biz.log.repository.CommandLogRepository;
import com.codeages.javaskeletonserver.biz.log.service.CommandLogService;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogSearchParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogUpdateParams;
import com.codeages.javaskeletonserver.biz.log.mapper.CommandLogMapper;

import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Validator;

@Service
public class CommandLogServiceImpl implements CommandLogService {

    private final CommandLogRepository commandLogRepository;

    private final CommandLogMapper commandLogMapper;

    private final Validator validator;

    public CommandLogServiceImpl(CommandLogRepository commandLogRepository,
                                 CommandLogMapper commandLogMapper,
                                 Validator validator) {
        this.commandLogRepository = commandLogRepository;
        this.commandLogMapper = commandLogMapper;
        this.validator = validator;
    }

    @Override
    public Page<CommandLogDto> search(CommandLogSearchParams searchParams, Pageable pageable) {
        QCommandLog q = QCommandLog.commandLog;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getSessionId())) {
            builder.and(q.sessionId.eq(searchParams.getSessionId()));
        }
        if (searchParams.getSessionId() != null) {
            builder.and(q.sessionId.eq(searchParams.getSessionId()));
        }
        if (StrUtil.isNotEmpty(searchParams.getCommandData())) {
            builder.and(q.commandData.eq(searchParams.getCommandData()));
        }
        if (searchParams.getCommandData() != null) {
            builder.and(q.commandData.eq(searchParams.getCommandData()));
        }
        return commandLogRepository.findAll(builder, pageable).map(commandLogMapper::toDto);
    }

    @Override
    public void create(CommandLogCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        commandLogRepository.save(commandLogMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(CommandLogUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var commandLog = commandLogRepository.findById(updateParams.getId())
                                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        commandLogMapper.toUpdateEntity(commandLog, updateParams);
        commandLogRepository.save(commandLog);
    }

    @Override
    public void delete(Long id) {
        commandLogRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        commandLogRepository.deleteById(id);
    }
}


