package com.codeages.termiusplus.biz.snippet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.snippet.entity.QCommand;
import com.codeages.termiusplus.biz.snippet.repository.CommandRepository;
import com.codeages.termiusplus.biz.snippet.service.CommandService;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandSearchParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import com.codeages.termiusplus.biz.snippet.mapper.CommandMapper;

import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Validator;

@Service
public class CommandServiceImpl implements CommandService {

    private final CommandRepository commandRepository;

    private final CommandMapper commandMapper;

    private final Validator validator;

    public CommandServiceImpl(CommandRepository commandRepository, CommandMapper commandMapper, Validator validator) {
        this.commandRepository = commandRepository;
        this.commandMapper = commandMapper;
        this.validator = validator;
    }

    @Override
    public Page<CommandDto> search(CommandSearchParams searchParams, Pageable pageable) {
        QCommand q = QCommand.command1;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getName())) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (searchParams.getName() != null) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (StrUtil.isNotEmpty(searchParams.getCommand())) {
            builder.and(q.command.eq(searchParams.getCommand()));
        }
        if (searchParams.getCommand() != null) {
            builder.and(q.command.eq(searchParams.getCommand()));
        }
        if (StrUtil.isNotEmpty(searchParams.getRemark())) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        if (searchParams.getRemark() != null) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        return commandRepository.findAll(builder, pageable).map(commandMapper::toDto);
    }

    @Override
    public void create(CommandCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        commandRepository.save(commandMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(CommandUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var command = commandRepository.findById(updateParams.getId())
                                       .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        commandMapper.toUpdateEntity(command, updateParams);
        commandRepository.save(command);
    }

    @Override
    public void delete(Long id) {
        commandRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        commandRepository.deleteById(id);
    }
}


