package com.codeages.termiusplus.biz.snippet.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandSearchParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import com.codeages.termiusplus.biz.snippet.entity.Command;
import com.codeages.termiusplus.biz.snippet.mapper.CommandMapper;
import com.codeages.termiusplus.biz.snippet.repository.CommandRepository;
import com.codeages.termiusplus.biz.snippet.service.CommandService;
import com.codeages.termiusplus.exception.AppException;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
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
        Specification<Command> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotEmpty(searchParams.getName())) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            if (searchParams.getName() != null) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            if (StrUtil.isNotEmpty(searchParams.getCommand())) {
                predicates.add(criteriaBuilder.equal(root.get("command"), searchParams.getCommand()));
            }
            if (searchParams.getCommand() != null) {
                predicates.add(criteriaBuilder.equal(root.get("command"), searchParams.getCommand()));
            }
            if (StrUtil.isNotEmpty(searchParams.getRemark())) {
                predicates.add(criteriaBuilder.equal(root.get("remark"), searchParams.getRemark()));
            }
            if (searchParams.getRemark() != null) {
                predicates.add(criteriaBuilder.equal(root.get("remark"), searchParams.getRemark()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return commandRepository.findAll(specification, pageable).map(commandMapper::toDto);
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


