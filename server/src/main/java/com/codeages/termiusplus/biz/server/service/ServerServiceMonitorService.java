package com.codeages.termiusplus.biz.server.service;

import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorCreateParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorDto;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorSearchParams;
import com.codeages.termiusplus.biz.server.dto.ServerServiceMonitorUpdateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ServerServiceMonitorService {

    Page<ServerServiceMonitorDto> search(ServerServiceMonitorSearchParams searchParams, Pageable pageable);

    void create(ServerServiceMonitorCreateParams createParams);

    void update(ServerServiceMonitorUpdateParams updateParams);

    void delete(Long id);
}


