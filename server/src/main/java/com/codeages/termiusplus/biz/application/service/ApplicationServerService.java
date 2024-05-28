package com.codeages.termiusplus.biz.application.service;

import com.codeages.termiusplus.biz.application.dto.ApplicationServerCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerSearchParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationServerUpdateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ApplicationServerService {

    Page<ApplicationServerDto> search(ApplicationServerSearchParams searchParams, Pageable pageable);

    void create(ApplicationServerCreateParams createParams);

    void update(ApplicationServerUpdateParams updateParams);

    void delete(Long id);

    void deleteByApplicationId(Long applicationId);
}


