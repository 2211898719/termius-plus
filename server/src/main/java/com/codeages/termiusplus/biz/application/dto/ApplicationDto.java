package com.codeages.termiusplus.biz.application.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class ApplicationDto {

    private Long id;

    private Long proxyId;

    private Long parentId;


    private Boolean isGroup;


    private Long sort;


    private String name;


    private String icon;

    private String masterMobile;
    private BigDecimal latitude;
    private BigDecimal longitude;

    private String content;

    private String identity;

    private List<ApplicationServerDto> serverList;
}

