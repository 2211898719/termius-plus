package com.codeages.termiusplus.biz.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * 应用监控日志
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ServerRunLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 服务器ID
     */
    private Long serverId;
    /**
     * 日期
     */
    private Date date;
    /**
     * CPU使用率
     */
    private String cpuUsage;
    /**
     * 内存使用率
     */
    private String memoryUsage;
    /**
     * 磁盘使用率
     */
    private String diskUsages;
    /**
     * 网络使用率
     */
    private String networkUsages;
    /**
     * 创建时间
     */
    @CreatedDate
    private Long createdAt;
    /**
     * 更新时间
     */
    @LastModifiedDate
    private Long updatedAt;
}

