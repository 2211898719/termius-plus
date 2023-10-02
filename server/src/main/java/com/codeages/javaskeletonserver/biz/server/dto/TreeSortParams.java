package com.codeages.javaskeletonserver.biz.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TreeSortParams {
    private Long id;
    @JsonIgnore
    private Long parentId;
    @JsonIgnore
    private Long sort;
    private List<TreeSortParams> children;
}

