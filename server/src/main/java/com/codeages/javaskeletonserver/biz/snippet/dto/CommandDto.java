package com.codeages.javaskeletonserver.biz.snippet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandDto {
    private Long id;
    private String name;
    private String command;
    private String remark;
}

