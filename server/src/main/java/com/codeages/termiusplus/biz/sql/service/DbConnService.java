package com.codeages.termiusplus.biz.sql.service;

import com.codeages.termiusplus.biz.sql.dto.DbConnDto;
import com.codeages.termiusplus.biz.sql.dto.DbConnCreateParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnUpdateParams;
import com.codeages.termiusplus.biz.sql.dto.DbConnSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface DbConnService {

    Page<DbConnDto> search(DbConnSearchParams searchParams, Pageable pageable);

    void create(DbConnCreateParams createParams);

    void update(DbConnUpdateParams updateParams);

    void delete(Long id);

    Optional<DbConnDto> findById(Long id);
}


