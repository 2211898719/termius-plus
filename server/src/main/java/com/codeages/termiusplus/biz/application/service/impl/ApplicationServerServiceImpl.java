package com.codeages.termiusplus.biz.application.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerSearchParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerUpdateParams;
import com.codeages.termiusplus.biz.application.entity.ApplicationServer;
import com.codeages.termiusplus.biz.application.mapper.ApplicationServerMapper;
import com.codeages.termiusplus.biz.application.repository.ApplicationServerRepository;
import com.codeages.termiusplus.biz.application.service.ApplicationServerService;
import com.codeages.termiusplus.biz.server.dto.ServerDto;
import com.codeages.termiusplus.biz.server.service.ServerService;
import com.codeages.termiusplus.biz.util.QueryUtils;
import com.codeages.termiusplus.exception.AppException;
import jakarta.persistence.criteria.Predicate;
import jakarta.validation.Validator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ApplicationServerServiceImpl implements ApplicationServerService {

    private final ApplicationServerRepository applicationServerRepository;

    private final ApplicationServerMapper applicationServerMapper;

    private final Validator validator;

    private final ServerService serverService;

    public ApplicationServerServiceImpl(ApplicationServerRepository applicationServerRepository,
                                        ApplicationServerMapper applicationServerMapper,
                                        Validator validator, ServerService serverService) {
        this.applicationServerRepository = applicationServerRepository;
        this.applicationServerMapper = applicationServerMapper;
        this.validator = validator;
        this.serverService = serverService;
    }

    @Override
    public Page<ApplicationServerDto> search(ApplicationServerSearchParams searchParams, Pageable pageable) {
        Specification<ApplicationServer> specification = (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (searchParams.getApplicationId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("applicationId"), searchParams.getApplicationId()));
            }
            if (searchParams.getServerId() != null) {
                predicates.add(criteriaBuilder.equal(root.get("serverId"), searchParams.getServerId()));
            }
            if (StrUtil.isNotEmpty(searchParams.getTag())) {
                predicates.add(criteriaBuilder.equal(root.get("tag"), searchParams.getTag()));
            }
            if (StrUtil.isNotEmpty(searchParams.getRemark())) {
                predicates.add(criteriaBuilder.equal(root.get("remark"), searchParams.getRemark()));
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
        Page<ApplicationServerDto> page = applicationServerRepository.findAll(specification, pageable)
                                                                    .map(applicationServerMapper::toDto);
        QueryUtils.batchQueryOneToOne(
                page.getContent(),
                ApplicationServerDto::getServerId,
                serverService::findByIdIn,
                ServerDto::getId,
                ApplicationServerDto::setServer
        );

        return page;
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

    @Override
    public void deleteByApplicationId(Long applicationId) {
        applicationServerRepository.deleteByApplicationId(applicationId);
    }

    @Override
    public List<ApplicationServerDto> getServers(Long applicationId) {
        return applicationServerRepository.findByApplicationId(applicationId)
                                          .stream()
                                          .map(applicationServerMapper::toDto)
                                          .collect(Collectors.toList());
    }
}


