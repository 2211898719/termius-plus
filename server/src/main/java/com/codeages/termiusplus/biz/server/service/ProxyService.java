package com.codeages.termiusplus.biz.server.service;

import com.codeages.termiusplus.biz.server.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface ProxyService {

    Page<ProxyDto> search(ProxySearchParams searchParams, Pageable pageable);

    ProxyDto create(ProxyCreateParams createParams);

    void update(ProxyUpdateParams updateParams);

    void delete(Long id);

    Optional<ProxyDto> findById(Long proxyId);

    ClashProxyDTO getClashProxy();

    ClashProxyDTO syncClashProxy();
}


