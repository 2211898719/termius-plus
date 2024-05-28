package com.codeages.termiusplus.biz.application.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 应用
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Application {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 父级ID
     */
    private Long parentId;
    /**
     * 是否为分组
     */
    private Boolean isGroup;
    /**
     * 排序
     */
    private Long sort;
    /**
     * 应用名称
     */
    private String name;
    /**
     * 应用图标
     */
    private String icon;
    /**
     * 应用详情/链接
     */
    private String content;
    /**
     * 应用身份
     */
    private String identity;
    /**
     * 负责人手机号
     */
    private String masterMobile;
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

