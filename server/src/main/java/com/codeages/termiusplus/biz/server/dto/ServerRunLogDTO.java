package com.codeages.termiusplus.biz.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ServerRunLogDTO {
    private Long id;
    private Long serverId;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date date;
    private String cpuUsage;
    private String memoryUsage;
    private String diskUsages;
    private String networkUsages;

    private String serverName;
    private String serverIp;
    private Long serverPort;
}

