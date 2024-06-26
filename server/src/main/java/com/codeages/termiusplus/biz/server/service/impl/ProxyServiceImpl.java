package com.codeages.termiusplus.biz.server.service.impl;

import cn.hutool.core.util.StrUtil;
import com.codeages.termiusplus.biz.ErrorCode;
import com.codeages.termiusplus.biz.server.dto.ProxyCreateParams;
import com.codeages.termiusplus.biz.server.dto.ProxyDto;
import com.codeages.termiusplus.biz.server.dto.ProxySearchParams;
import com.codeages.termiusplus.biz.server.dto.ProxyUpdateParams;
import com.codeages.termiusplus.biz.server.entity.QProxy;
import com.codeages.termiusplus.biz.server.mapper.ProxyMapper;
import com.codeages.termiusplus.biz.server.repository.ProxyRepository;
import com.codeages.termiusplus.biz.server.service.ProxyService;
import com.codeages.termiusplus.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Validator;
import java.util.Optional;

@Service
public class ProxyServiceImpl implements ProxyService {

    private final ProxyRepository proxyRepository;

    private final ProxyMapper proxyMapper;

    private final Validator validator;

    private final HttpServletResponse response;

    public ProxyServiceImpl(ProxyRepository proxyRepository, ProxyMapper proxyMapper, Validator validator,
                            HttpServletResponse response) {
        this.proxyRepository = proxyRepository;
        this.proxyMapper = proxyMapper;
        this.validator = validator;
        this.response = response;
    }

    @Override
    public Page<ProxyDto> search(ProxySearchParams searchParams, Pageable pageable) {
        QProxy q = QProxy.proxy;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getIp())) {
            builder.and(q.ip.eq(searchParams.getIp()));
        }
        if (searchParams.getIp() != null) {
            builder.and(q.ip.eq(searchParams.getIp()));
        }

        if (StrUtil.isNotEmpty(searchParams.getPassword())) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }
        if (searchParams.getPassword() != null) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }
        return proxyRepository.findAll(builder, pageable).map(proxyMapper::toDto);
    }

    @Override
    public ProxyDto create(ProxyCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        return proxyMapper.toDto(proxyRepository.save(proxyMapper.toCreateEntity(createParams)));
    }

    @Override
    public void update(ProxyUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var proxy = proxyRepository.findById(updateParams.getId())
                                   .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        proxyMapper.toUpdateEntity(proxy, updateParams);
        proxyRepository.save(proxy);
    }

    @Override
    public void delete(Long id) {
        proxyRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        proxyRepository.deleteById(id);
    }

    @Override
    public Optional<ProxyDto> findById(Long proxyId) {
        return proxyRepository.findById(proxyId).map(proxyMapper::toDto);
    }



}


