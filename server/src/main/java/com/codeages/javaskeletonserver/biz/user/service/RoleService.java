package com.codeages.javaskeletonserver.biz.user.service;

import com.codeages.javaskeletonserver.biz.user.dto.RoleDto;
import com.codeages.javaskeletonserver.biz.user.dto.RoleCreateParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleUpdateParams;
import com.codeages.javaskeletonserver.biz.user.dto.RoleSearchParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface RoleService {

    Page<RoleDto> search(RoleSearchParams searchParams, Pageable pageable);

    void create(RoleCreateParams createParams);

    void update(RoleUpdateParams updateParams);

    void delete(Long id);

    List<RoleDto> findByIds(List<Long> ids);

    RoleDto findByName(String name);
}


