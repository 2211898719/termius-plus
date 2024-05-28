package com.codeages.termiusplus.biz.snippet.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 命令
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Command {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 命令名称
     */
    private String name;
    /**
     * 命令
     */
    private String command;
    /**
     * 备注
     */
    private String remark;
    /**
     * 服务器ids
     */
    private String serverIds;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;
}

