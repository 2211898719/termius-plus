package com.codeages.termiusplus.biz.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandCreateParams {
    private String name;
    private String command;
    private String remark;
    private String serverIds;
}

