package com.codeages.termiusplus.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortForwarderDto {
    private String forwardingName;
    private LocalPortForwarder localPortForwarder;
    private Integer localPort;
    private String localHost;
    private String remoteHost;
    private Integer remotePort;
    private Long serverId;
    private ServerDto serverDto;
}
