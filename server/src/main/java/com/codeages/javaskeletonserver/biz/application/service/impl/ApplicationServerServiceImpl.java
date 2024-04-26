package com.codeages.javaskeletonserver.biz.application.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerSearchParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationServerUpdateParams;
import com.codeages.javaskeletonserver.biz.application.entity.QApplicationServer;
import com.codeages.javaskeletonserver.biz.application.mapper.ApplicationServerMapper;
import com.codeages.javaskeletonserver.biz.application.repository.ApplicationServerRepository;
import com.codeages.javaskeletonserver.biz.application.service.ApplicationServerService;
import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.validation.Validator;

@Service
public class ApplicationServerServiceImpl implements ApplicationServerService {

    private final ApplicationServerRepository applicationServerRepository;

    private final ApplicationServerMapper applicationServerMapper;

    private final Validator validator;

    public ApplicationServerServiceImpl(ApplicationServerRepository applicationServerRepository,
                                        ApplicationServerMapper applicationServerMapper,
                                        Validator validator) {
        this.applicationServerRepository = applicationServerRepository;
        this.applicationServerMapper = applicationServerMapper;
        this.validator = validator;
    }

    @Override
    public Page<ApplicationServerDto> search(ApplicationServerSearchParams searchParams, Pageable pageable) {
        QApplicationServer q = QApplicationServer.applicationServer;
        BooleanBuilder builder = new BooleanBuilder();
        if (searchParams.getApplicationId() != null) {
            builder.and(q.applicationId.eq(searchParams.getApplicationId()));
        }
        if (searchParams.getServerId() != null) {
            builder.and(q.serverId.eq(searchParams.getServerId()));
        }
        if (StrUtil.isNotEmpty(searchParams.getTag())) {
            builder.and(q.tag.eq(searchParams.getTag()));
        }
        if (StrUtil.isNotEmpty(searchParams.getRemark())) {
            builder.and(q.remark.eq(searchParams.getRemark()));
        }
        return applicationServerRepository.findAll(builder, pageable).map(applicationServerMapper::toDto);
    }

    @Override
    public void create(ApplicationServerCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        applicationServerRepository.save(applicationServerMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(ApplicationServerUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var applicationServer = applicationServerRepository.findById(updateParams.getId())
                                                           .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        applicationServerMapper.toUpdateEntity(applicationServer, updateParams);
        applicationServerRepository.save(applicationServer);
    }

    @Override
    public void delete(Long id) {
        applicationServerRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        applicationServerRepository.deleteById(id);
    }
}


