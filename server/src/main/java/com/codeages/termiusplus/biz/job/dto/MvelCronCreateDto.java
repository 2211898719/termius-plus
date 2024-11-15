package com.codeages.termiusplus.biz.job.dto;

import com.codeages.termiusplus.biz.server.dto.ServerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MvelCronCreateDto {
    private String jobName;
    private String jobGroup;
    private List<Long> serverIds;
    private List<ServerDto> serverDtoList;
    private List<String> params;
    private String mvelScript;
    private String cronExpression;
}
