package com.codeages.javaskeletonserver.biz.server.service;

import cn.hutool.core.lang.tree.Tree;
import com.codeages.javaskeletonserver.biz.server.dto.*;

import java.util.List;

public interface ServerService {

    List<Tree<Long>> findAll();

    void create(ServerCreateParams serverCreateParams);

    void update(ServerUpdateParams serverUpdateParams);

    void delete(Long id);

    void sort(List<TreeSortParams> treeSortParams);

}


