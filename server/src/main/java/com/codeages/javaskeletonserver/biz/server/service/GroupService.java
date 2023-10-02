package com.codeages.javaskeletonserver.biz.server.service;

import com.codeages.javaskeletonserver.biz.server.dto.GroupCreateParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupDto;
import com.codeages.javaskeletonserver.biz.server.dto.GroupSearchParams;
import com.codeages.javaskeletonserver.biz.server.dto.GroupUpdateParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface GroupService {

    Page<GroupDto> search(GroupSearchParams searchParams, Pageable pageable);

    void create(GroupCreateParams createParams);

    void update(GroupUpdateParams updateParams);

    void delete(Long id);
}


