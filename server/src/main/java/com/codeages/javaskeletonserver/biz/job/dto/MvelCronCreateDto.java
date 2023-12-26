package com.codeages.javaskeletonserver.biz.job.dto;

import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MvelCronCreateDto {
    private String jobName;
    private String jobGroup;
    private List<Long> serverIds;
    private List<ServerDto> serverDtos;
    private List<String> params;
    private String mvelScript;
    private String cronExpression;
}
