package com.codeages.javaskeletonserver.biz.application.entity;

import com.codeages.javaskeletonserver.biz.application.enums.ApplicationMonitorTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * 应用监控
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApplicationMonitor {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 应用ID
     */
    private Long applicationId;
    /**
     * 监控类型
     */
    @Enumerated(EnumType.STRING)
    private ApplicationMonitorTypeEnum type;
    /**
     * 监控配置
     */
    private String config;
    /**
     * 备注
     */
    private String remark;
    private Long failureCount;
    private Date failureTime;
    private String responseResult;
    private Long responseTime;
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

