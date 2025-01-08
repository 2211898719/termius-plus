package com.codeages.termiusplus.biz.snippet.service;

import com.codeages.termiusplus.biz.snippet.dto.CommandCreateParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandDto;
import com.codeages.termiusplus.biz.snippet.dto.CommandSearchParams;
import com.codeages.termiusplus.biz.snippet.dto.CommandUpdateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CommandService {

    Page<CommandDto> search(CommandSearchParams searchParams, Pageable pageable);

    void create(CommandCreateParams createParams);

    void update(CommandUpdateParams updateParams);

    void delete(Long id);

    void execute(Long id);
}


