package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationSearchParams {


    private Long parentId;


    private Boolean isGroup;


    private Long sort;


    private String name;


    private String icon;


    private String content;


    private String identity;
}

