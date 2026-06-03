package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class PatrolScriptCreateParams {
    @NotBlank
    private String name;
    private String description;
    @NotBlank
    private String scriptContent;
    private String outputSchema;
    private String category = "custom";
    private String serverIds;
    private String groupIds;
}
