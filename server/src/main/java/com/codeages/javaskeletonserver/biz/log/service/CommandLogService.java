package com.codeages.javaskeletonserver.biz.log.service;

import com.codeages.javaskeletonserver.biz.log.dto.CommandLogDto;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogCreateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogUpdateParams;
import com.codeages.javaskeletonserver.biz.log.dto.CommandLogSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandLogService {

    Page<CommandLogDto> search(CommandLogSearchParams searchParams, Pageable pageable);

    void create(CommandLogCreateParams createParams);

    void update(CommandLogUpdateParams updateParams);

    void delete(Long id);
}


