package com.codeages.termiusplus.biz.patrol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Data
public class AgentChatParams {
    @NotBlank
    private String message;
    private Long serverId;
    private String serverIds;
    private String conversationId;

    public List<Long> getServerIdList() {
        if (serverIds != null && !serverIds.isBlank()) {
            return Arrays.stream(serverIds.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(Long::parseLong)
                    .collect(Collectors.toList());
        }
        if (serverId != null) {
            return Collections.singletonList(serverId);
        }
        return Collections.emptyList();
    }
}
