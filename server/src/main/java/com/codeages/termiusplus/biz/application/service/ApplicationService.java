package com.codeages.termiusplus.biz.application.service;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.termiusplus.biz.application.dto.ApplicationCreateParams;
import com.codeages.termiusplus.biz.application.dto.ApplicationDto;
import com.codeages.termiusplus.biz.application.dto.ApplicationUpdateParams;
import com.codeages.termiusplus.biz.server.dto.TreeSortParams;

import java.util.Collection;
import java.util.List;

public interface ApplicationService {

    List<Tree<Long>> findAll();

    List<ApplicationDto> findAllApplication();

    void create(ApplicationCreateParams createParams);

    void update(ApplicationUpdateParams updateParams);

    void delete(Long id);

    void sort(List<TreeSortParams> treeSortParams);

    List<ApplicationDto> findAllByIds(Collection<Long> ids);

    List<Tree<Long>> groupList();
}


