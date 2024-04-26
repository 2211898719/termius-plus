package com.codeages.javaskeletonserver.biz.application.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorSearchParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationMonitorUpdateParams;
import com.codeages.javaskeletonserver.biz.application.entity.QApplicationMonitor;
import com.codeages.javaskeletonserver.biz.application.mapper.ApplicationMonitorMapper;
import com.codeages.javaskeletonserver.biz.application.repository.ApplicationMonitorRepository;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationMonitorService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.validation.Validator;
import java.util.Optional;

@Service
public class ApplicationMonitorServiceImpl implements ApplicationMonitorService {

    private final ApplicationMonitorRepository applicationMonitorRepository;

    private final ApplicationMonitorMapper applicationMonitorMapper;

    private final Validator validator;

    public ApplicationMonitorServiceImpl(ApplicationMonitorRepository applicationMonitorRepository,
                                         ApplicationMonitorMapper applicationMonitorMapper,
                                         Validator validator) {
        this.applicationMonitorRepository = applicationMonitorRepository;
        this.applicationMonitorMapper = applicationMonitorMapper;
        this.validator = validator;
    }

    @Override
    public Page<ApplicationMonitorDto> search(ApplicationMonitorSearchParams searchParams, Pageable pageable) {
        QApplicationMonitor q = QApplicationMonitor.applicationMonitor;
        BooleanBuilder builder = new BooleanBuilder();
        if (searchParams.getApplicationId() != null) {
            builder.and(q.applicationId.eq(searchParams.getApplicationId()));
        }
        if (StrUtil.isNotEmpty(searchParams.getType())) {
            builder.and(q.type.eq(searchParams.getType()));
        }
        if (StrUtil.isNotEmpty(searchParams.getConfig())) {
            builder.and(q.config.eq(searchParams.getConfig()));
        }
        if (StrUtil.isNotEmpty(searchParams.getRemark())) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        return applicationMonitorRepository.findAll(builder, pageable).map(applicationMonitorMapper::toDto);
    }

    @Override
    public void create(ApplicationMonitorCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        applicationMonitorRepository.save(applicationMonitorMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(ApplicationMonitorUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var applicationMonitor = applicationMonitorRepository.findById(updateParams.getId())
                                                             .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        applicationMonitorMapper.toUpdateEntity(applicationMonitor, updateParams);
        applicationMonitorRepository.save(applicationMonitor);
    }

    @Override
    public void delete(Long id) {
        applicationMonitorRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        applicationMonitorRepository.deleteById(id);
    }

    @Override
    public void deleteByApplicationId(Long applicationId) {
        applicationMonitorRepository.deleteByApplicationId(applicationId);
    }

    @Override
    public Optional<ApplicationMonitorDto> getByApplicationId(Long applicationId) {
        return applicationMonitorRepository.findByApplicationId(applicationId)
                                           .map(applicationMonitorMapper::toDto);
    }
}


