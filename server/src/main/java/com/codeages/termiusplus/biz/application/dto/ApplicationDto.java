package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationDto {

    private Long id;

    private Long proxyId;

    private Long parentId;


    private Boolean isGroup;


    private Long sort;


    private String name;


    private String icon;

    private String masterMobile;

    private String content;

    private String identity;

    private List<ApplicationServerDto> serverList;
}

