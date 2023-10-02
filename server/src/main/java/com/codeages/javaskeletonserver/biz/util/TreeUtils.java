package com.codeages.javaskeletonserver.biz.util;

import cn.hutool.core.collection.CollectionUtil;
import com.codeages.javaskeletonserver.biz.server.dto.TreeSortParams;

import java.util.List;

public class TreeUtils {

    public static void rebuildSeq(TreeSortParams treeSortParams) {
        List<TreeSortParams> children = treeSortParams.getChildren();
        if (CollectionUtil.isEmpty(children)) {
            return;
        }

        for (int i = 0; i < children.size(); i++) {
            rebuildSeq(treeSortParams.getChildren().get(i));

            treeSortParams.getChildren().get(i).setSort((long) i);
        }
    }


}
