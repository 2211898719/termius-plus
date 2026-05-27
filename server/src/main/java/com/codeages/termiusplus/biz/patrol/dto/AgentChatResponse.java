package com.codeages.termiusplus.biz.patrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AgentChatResponse {
    private String reply;
    private boolean needsConfirmation;
    private String pendingCommand;
}
