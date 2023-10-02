package com.codeages.javaskeletonserver.biz.server.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 组
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Group {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 组名
     */
    private String name;
    /**
     * 父组ID
     */
    private Long parentId;
    /**
     * 代理ID
     */
    private Long proxyId;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;
}

