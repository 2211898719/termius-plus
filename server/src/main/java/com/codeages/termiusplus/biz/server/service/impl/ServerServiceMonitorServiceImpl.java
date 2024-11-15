package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorDto;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorSearchParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorUpdateParams;
import com.codeages.termiusplus.biz.server.entity.QServerServiceMonitor;
import com.codeages.termiusplus.biz.server.mapper.ServerServiceMonitorMapper;
import com.codeages.termiusplus.biz.server.repository.ServerServiceMonitorRepository;
import com.codeages.termiusplus.biz.server.service.ServerServiceMonitorService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class ServerServiceMonitorServiceImpl implements ServerServiceMonitorService {

    private final ServerServiceMonitorRepository serverServiceMonitorRepository;

    private final ServerServiceMonitorMapper serverServiceMonitorMapper;

    private final Validator validator;

    public ServerServiceMonitorServiceImpl(ServerServiceMonitorRepository serverServiceMonitorRepository,
                                           ServerServiceMonitorMapper serverServiceMonitorMapper,
                                           Validator validator) {
        this.serverServiceMonitorRepository = serverServiceMonitorRepository;
        this.serverServiceMonitorMapper = serverServiceMonitorMapper;
        this.validator = validator;
    }

    @Override
    public Page<ServerServiceMonitorDto> search(ServerServiceMonitorSearchParams searchParams, Pageable pageable) {
        QServerServiceMonitor q = QServerServiceMonitor.serverServiceMonitor;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getName())) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        if (searchParams.getName() != null) {
            builder.and(q.name.eq(searchParams.getName()));
        }
        return serverServiceMonitorRepository.findAll(builder, pageable).map(serverServiceMonitorMapper::toDto);
    }

    @Override
    public void create(ServerServiceMonitorCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        serverServiceMonitorRepository.save(serverServiceMonitorMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(ServerServiceMonitorUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var serverServiceMonitor = serverServiceMonitorRepository.findById(updateParams.getId())
                                                                 .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        serverServiceMonitorMapper.toUpdateEntity(serverServiceMonitor, updateParams);
        serverServiceMonitorRepository.save(serverServiceMonitor);
    }

    @Override
    public void delete(Long id) {
        serverServiceMonitorRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        serverServiceMonitorRepository.deleteById(id);
    }
}


