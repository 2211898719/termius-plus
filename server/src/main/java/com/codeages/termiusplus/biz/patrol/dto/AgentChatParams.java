package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AgentChatParams {
    @NotBlank
    private String message;
    private Long serverId;
}
