package com.codeages.termiusplus.biz.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.util.Date;

/**
 * 应用监控日志
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApplicationMonitorLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 应用ID
     */
    private Long applicationId;
    /**
     * 日期
     */
    private Date date;
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

