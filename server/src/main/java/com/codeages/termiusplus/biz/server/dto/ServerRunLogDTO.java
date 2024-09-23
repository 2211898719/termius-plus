package com.codeages.termiusplus.biz.server.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.List;

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
    private ServerDto server;

    private Boolean infoStatus;
    private String serverName;
    private String serverIp;
    private Long serverPort;
    private List<ServerRunLogDTO> detail;

    public ServerRunLogDTO(ServerDto server ,Boolean infoStatus, String serverName, String serverIp, Long serverPort) {
        this.server = server;
        this.infoStatus = infoStatus;
        this.serverName = serverName;
        this.serverIp = serverIp;
        this.serverPort = serverPort;
    }
}

