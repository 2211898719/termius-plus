package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PatrolExecuteDraftParams {
    @NotNull
    private Long serverId;
    private String scriptContent;
    private String scriptName;
}
