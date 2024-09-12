package com.codeages.termiusplus.biz.application.service;

import com.codeages.termiusplus.biz.application.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface ApplicationMonitorService {

    Page<ApplicationMonitorDto> search(ApplicationMonitorSearchParams searchParams, Pageable pageable);

    void create(ApplicationMonitorCreateParams createParams);

    void update(ApplicationMonitorUpdateParams updateParams);

    void delete(Long id);

    void deleteByApplicationId(Long applicationId);

    Optional<ApplicationMonitorDto> getByApplicationId(Long applicationId);

    ApplicationMonitorExecDto exec(ApplicationMonitorDto createParams);

    void updateStatusAndSendMessage(ApplicationMonitorDto applicationMonitorDto, ApplicationMonitorExecDto testDto);

    List<ApplicationMonitorLogCountDto> getApplicationErrorRank();
}


