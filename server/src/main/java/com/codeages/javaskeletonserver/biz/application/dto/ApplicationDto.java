package com.codeages.javaskeletonserver.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {

    private Long id;


    private Long parentId;


    private Boolean isGroup;


    private Long sort;


    private String name;


    private String icon;

    private String masterMobile;

    private String content;


    private String identity;
}

