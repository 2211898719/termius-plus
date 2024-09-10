package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AIChatParams {
   private String sessionId;
   private String message;
   private String prompt;
}
