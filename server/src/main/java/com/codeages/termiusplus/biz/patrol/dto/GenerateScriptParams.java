package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GenerateScriptParams {
    @NotBlank
    private String description;
}
