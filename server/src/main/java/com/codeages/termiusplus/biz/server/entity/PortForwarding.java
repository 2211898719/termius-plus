package com.codeages.termiusplus.biz.server.entity;

import com.codeages.termiusplus.biz.server.enums.PortForWardingStatusEnum;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

/**
 * 端口转发
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class PortForwarding {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long serverId;

    private String forwardingName;

    private String localHost;

    private Integer localPort;

    private String remoteHost;

    private Integer remotePort;

    @Enumerated(EnumType.STRING)
    private PortForWardingStatusEnum status;

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;
}
