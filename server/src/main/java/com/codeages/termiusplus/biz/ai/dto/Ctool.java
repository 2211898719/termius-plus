package com.codeages.termiusplus.biz.ai.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class Ctool {
    private Long code;
    private String message;
    private String sid;
    private String id;
    private Long created;
    private List<Choice> choices;
}
