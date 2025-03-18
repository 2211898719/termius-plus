package com.codeages.termiusplus.biz.application.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationServerUpdateParams {

    private Long id;

    @NotNull(message = "应用ID不能为空")

    private Long applicationId;

    @NotNull(message = "服务器ID不能为空")

    private Long serverId;


    private String tag;

    private String remark;
}

