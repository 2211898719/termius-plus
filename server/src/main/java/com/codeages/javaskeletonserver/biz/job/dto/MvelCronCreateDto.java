package com.codeages.javaskeletonserver.biz.job.dto;

import com.codeages.javaskeletonserver.biz.server.dto.ServerDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MvelCronCreateDto {
    private String jobName;
    private String jobGroup;
    private Long serverId;
    private ServerDto serverDto;
    private String mvelScript;
    private String cronExpression;
}
