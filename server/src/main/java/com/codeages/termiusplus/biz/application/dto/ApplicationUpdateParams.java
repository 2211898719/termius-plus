package com.codeages.termiusplus.biz.application.dto;

import com.codeages.termiusplus.biz.application.enums.ApplicationMonitorTypeEnum;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationUpdateParams {

    private Long id;

    @NotNull(message = "父级ID不能为空")

    private Long parentId;

    private Long proxyId;

    @NotNull(message = "是否为分组不能为空")

    private Boolean isGroup;

    @NotNull(message = "排序不能为空")

    private Long sort;

    @NotNull(message = "应用名称不能为空")

    private String name;


    private String icon;

    private String masterMobile;


    private String content;

    private BigDecimal latitude;
    private BigDecimal longitude;

    private String identity;

    private ApplicationMonitorTypeEnum monitorType;

    private String monitorConfig;

    private String remark;

    private List<ApplicationServerCreateParams> serverList;
}

