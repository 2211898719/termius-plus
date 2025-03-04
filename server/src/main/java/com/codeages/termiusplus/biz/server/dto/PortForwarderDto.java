package com.codeages.termiusplus.biz.server.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortForwarderDto {
    private Long id;
    private String forwardingName;
    @JsonIgnore
    private LocalPortForwarder localPortForwarder;
    private Integer localPort;
    private String localHost;
    private String remoteHost;
    private Integer remotePort;
    private Long serverId;
    private ServerDto serverDto;
    private Integer retryCount;
}
