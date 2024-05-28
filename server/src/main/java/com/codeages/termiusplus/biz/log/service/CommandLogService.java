package com.codeages.termiusplus.biz.log.service;

import com.codeages.termiusplus.biz.log.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandLogService {

    Page<CommandLogSimpleDto> search(CommandLogSearchParams searchParams, Pageable pageable);

    CommandLogDto create(CommandLogCreateParams createParams);

    void update(CommandLogUpdateParams updateParams);

    void delete(Long id);

    CommandLogDto get(Long id);
}


