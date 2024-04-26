package com.codeages.javaskeletonserver.biz.application.service;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationCreateParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationDto;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationSearchParams;
import com.codeages.javaskeletonserver.biz.application.dto.ApplicationUpdateParams;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ApplicationService {

    List<Tree<Long>> findAll();

    void create(ApplicationCreateParams createParams);

    void update(ApplicationUpdateParams updateParams);

    void delete(Long id);

    void sort(List<TreeSortParams> treeSortParams);
}


