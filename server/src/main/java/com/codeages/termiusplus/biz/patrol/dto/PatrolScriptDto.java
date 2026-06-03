package com.codeages.termiusplus.biz.patrol.dto;

import lombok.Data;

@Data
public class PatrolScriptDto {
    private Long id;
    private String name;
    private String description;
    private String scriptContent;
    private String outputSchema;
    private String category;
    private Boolean enabled;
    private String serverIds;
    private String groupIds;
    private Long createdAt;
    private Long updatedAt;
}
