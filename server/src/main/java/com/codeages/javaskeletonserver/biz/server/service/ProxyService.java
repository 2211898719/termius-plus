package com.codeages.javaskeletonserver.biz.server.service;

import com.codeages.javaskeletonserver.biz.server.dto.ProxyCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyDto;
import com.codeages.javaskeletonserver.biz.server.dto.ProxySearchParams;
import com.codeages.javaskeletonserver.biz.server.dto.ProxyUpdateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProxyService {

    Page<ProxyDto> search(ProxySearchParams searchParams, Pageable pageable);

    ProxyDto create(ProxyCreateParams createParams);

    void update(ProxyUpdateParams updateParams);

    void delete(Long id);
}


