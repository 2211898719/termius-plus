package com.codeages.javaskeletonserver.biz.server.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import net.schmizz.sshj.connection.channel.direct.LocalPortForwarder;
import net.schmizz.sshj.connection.channel.forwarded.RemotePortForwarder;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PortForwarderDto {
    private String forwardingName;
    private LocalPortForwarder localPortForwarder;
    private Integer localPort;
    private String localHost;
    private Integer remotePort;
    private Long serverId;
    private ServerDto serverDto;
}
