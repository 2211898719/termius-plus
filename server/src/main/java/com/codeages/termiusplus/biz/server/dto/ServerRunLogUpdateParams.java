package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerRunLogUpdateParams {
    private Long id;
    private Long serverId;
    private Date date;
    private String cpuUsage;
    private String memoryUsage;
    private String diskUsages;
    private String networkUsages;
}

