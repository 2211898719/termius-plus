package com.codeages.javaskeletonserver.biz.application.dto;

import com.codeages.javaskeletonserver.biz.application.enums.ApplicationMonitorTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotNull;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationCreateParams {

    @NotNull(message = "父级ID不能为空")

    private Long parentId;

    @NotNull(message = "是否为分组不能为空")

    private Boolean isGroup;

    @NotNull(message = "排序不能为空")

    private Long sort;

    @NotNull(message = "应用名称不能为空")

    private String name;


    private String icon;


    private String content;

    private String masterMobile;

    private String identity;

    private ApplicationMonitorTypeEnum monitorType;

    private String monitorConfig;

    private String remark;

    private List<ApplicationServerCreateParams> serverList;
}

