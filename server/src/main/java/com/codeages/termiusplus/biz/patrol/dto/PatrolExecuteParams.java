package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatrolExecuteParams {
    @NotNull
    private Long scriptId;
    @NotNull
    private Long serverId;
}
