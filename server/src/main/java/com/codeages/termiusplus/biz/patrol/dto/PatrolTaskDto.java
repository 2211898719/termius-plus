package com.codeages.termiusplus.biz.patrol.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class PatrolTaskDto {
    private Long id;
    private Long scriptId;
    private Long serverId;
    private String status;
    private String output;
    private Boolean alertSent;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date executedAt;
    private String scriptName;
    private String serverName;
}
