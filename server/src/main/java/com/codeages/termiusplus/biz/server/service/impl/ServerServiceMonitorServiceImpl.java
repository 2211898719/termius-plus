package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorDto;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorSearchParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorUpdateParams;
import com.codeages.termiusplus.biz.server.entity.ServerServiceMonitor;
import com.codeages.termiusplus.biz.server.mapper.ServerServiceMonitorMapper;
import com.codeages.termiusplus.biz.server.repository.ServerServiceMonitorRepository;
import com.codeages.termiusplus.biz.server.service.ServerServiceMonitorService;
import com.codeages.termiusplus.exception.AppException;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

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
        Specification<ServerServiceMonitor> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (StrUtil.isNotEmpty(searchParams.getName())) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            if (searchParams.getName() != null) {
                predicates.add(criteriaBuilder.equal(root.get("name"), searchParams.getName()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        return serverServiceMonitorRepository.findAll(specification, pageable).map(serverServiceMonitorMapper::toDto);
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


