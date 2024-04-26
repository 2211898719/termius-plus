package com.codeages.javaskeletonserver.biz.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 应用服务器
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ApplicationServer {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 应用ID
     */
    private Long applicationId;
    /**
     * 服务器ID
     */
    private Long serverId;
    /**
     * 标签
     */
    private String tag;
    /**
     * 备注
     */
    private String remark;
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

