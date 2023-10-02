package com.codeages.javaskeletonserver.biz.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.server.dto.GroupCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupDto;
import com.codeages.javaskeletonserver.biz.server.dto.GroupSearchParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupUpdateParams;
import com.codeages.javaskeletonserver.biz.server.entity.QGroup;
import com.codeages.javaskeletonserver.biz.server.mapper.GroupMapper;
import com.codeages.javaskeletonserver.biz.server.repository.GroupRepository;
import com.codeages.javaskeletonserver.biz.server.service.GroupService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class GroupServiceImpl implements GroupService {

    private final GroupRepository groupRepository;

    private final GroupMapper groupMapper;

    private final Validator validator;

    public GroupServiceImpl(GroupRepository groupRepository, GroupMapper groupMapper, Validator validator) {
        this.groupRepository = groupRepository;
        this.groupMapper = groupMapper;
        this.validator = validator;
    }

    @Override
    public Page<GroupDto> search(GroupSearchParams searchParams, Pageable pageable) {
        QGroup q = QGroup.group;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getName())) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (searchParams.getName() != null) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        return groupRepository.findAll(builder, pageable).map(groupMapper::toDto);
    }

    @Override
    public void create(GroupCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        groupRepository.save(groupMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(GroupUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var group = groupRepository.findById(updateParams.getId())
                                   .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        groupMapper.toUpdateEntity(group, updateParams);
        groupRepository.save(group);
    }

    @Override
    public void delete(Long id) {
        groupRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        groupRepository.deleteById(id);
    }
}


