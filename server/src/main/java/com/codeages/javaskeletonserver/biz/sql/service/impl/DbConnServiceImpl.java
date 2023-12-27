package com.codeages.javaskeletonserver.biz.sql.service.impl;

import cn.hutool.core.util.StrUtil;

import com.codeages.javaskeletonserver.biz.ErrorCode;
import com.codeages.javaskeletonserver.biz.sql.entity.QDbConn;
import com.codeages.javaskeletonserver.biz.sql.repository.DbConnRepository;
import com.codeages.javaskeletonserver.biz.sql.service.DbConnService;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnDto;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnSearchParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnCreateParams;
import com.codeages.javaskeletonserver.biz.sql.dto.DbConnUpdateParams;
import com.codeages.javaskeletonserver.biz.sql.mapper.DbConnMapper;

import com.codeages.javaskeletonserver.exception.AppException;
import com.querydsl.core.BooleanBuilder;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import javax.validation.Validator;
import java.util.Optional;

@Service
public class DbConnServiceImpl implements DbConnService {

    private final DbConnRepository dbConnRepository;

    private final DbConnMapper dbConnMapper;

    private final Validator validator;

    public DbConnServiceImpl(DbConnRepository dbConnRepository, DbConnMapper dbConnMapper, Validator validator) {
        this.dbConnRepository = dbConnRepository;
        this.dbConnMapper = dbConnMapper;
        this.validator = validator;
    }

    @Override
    public Page<DbConnDto> search(DbConnSearchParams searchParams, Pageable pageable) {
        QDbConn q = QDbConn.dbConn;
        BooleanBuilder builder = new BooleanBuilder();
        if (StrUtil.isNotEmpty(searchParams.getConnName())) {
            builder.and(q.connName.eq(searchParams.getConnName()));
        }
        if (searchParams.getConnName() != null) {
            builder.and(q.connName.eq(searchParams.getConnName()));
        }
        if (StrUtil.isNotEmpty(searchParams.getUsername())) {
            builder.and(q.username.eq(searchParams.getUsername()));
        }
        if (searchParams.getUsername() != null) {
            builder.and(q.username.eq(searchParams.getUsername()));
        }
        if (StrUtil.isNotEmpty(searchParams.getPassword())) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }
        if (searchParams.getPassword() != null) {
            builder.and(q.password.eq(searchParams.getPassword()));
        }
        if (StrUtil.isNotEmpty(searchParams.getHost())) {
            builder.and(q.host.eq(searchParams.getHost()));
        }
        if (searchParams.getHost() != null) {
            builder.and(q.host.eq(searchParams.getHost()));
        }
        if (StrUtil.isNotEmpty(searchParams.getPort())) {
            builder.and(q.port.eq(searchParams.getPort()));
        }
        if (searchParams.getPort() != null) {
            builder.and(q.port.eq(searchParams.getPort()));
        }
        return dbConnRepository.findAll(builder, pageable).map(dbConnMapper::toDto);
    }

    @Override
    public void create(DbConnCreateParams createParams) {
        var errors = validator.validate(createParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        dbConnRepository.save(dbConnMapper.toCreateEntity(createParams));
    }

    @Override
    public void update(DbConnUpdateParams updateParams) {
        var errors = validator.validate(updateParams);
        if (!errors.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_ARGUMENT, errors);
        }

        var dbConn = dbConnRepository.findById(updateParams.getId())
                                     .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));
        dbConnMapper.toUpdateEntity(dbConn, updateParams);
        dbConnRepository.save(dbConn);
    }

    @Override
    public void delete(Long id) {
        dbConnRepository.findById(id).orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND));

        dbConnRepository.deleteById(id);
    }

    @Override
    public Optional<DbConnDto> findById(Long id) {
        return dbConnRepository.findById(id).map(dbConnMapper::toDto);
    }
}


