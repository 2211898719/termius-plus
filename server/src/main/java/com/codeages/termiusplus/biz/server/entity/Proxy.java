package com.codeages.termiusplus.biz.server.entity;

import com.codeages.termiusplus.biz.server.enums.ProxyTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 代理
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Proxy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 代理IP
     */
    private String ip;
    /**
     * 名称
     */
    private String name;
    /**
     * 代理端口
     */
    private Long port;
    /**
     * 代理类型,HTTP,SOCKS5
     */
    @Enumerated(EnumType.STRING)
    private ProxyTypeEnum type;
    /**
     * 密码
     */
    private String password;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;
}

