package com.codeages.termiusplus.biz.server.entity;

import com.codeages.termiusplus.biz.server.enums.OSEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

/**
 * 服务器
 */
@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Server {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    /**
     * 服务器名
     */
    private String name;
    /**
     * 服务器IP/host
     */
    private String ip;
    private String firstCommand;
    /**
     * ssh服务器端口
     */
    private Long port;
    private Boolean isGroup;
    private Long parentId;
    private Long sort;
    /**
     * 密码
     */
    private String password;
    /**
     * 操作系统
     */
    @Enumerated(EnumType.STRING)
    private OSEnum os;
    /**
     * 用户名
     */
    private String username;
    private Boolean autoSudo;
    private Boolean isDb;
    private Boolean keepAlive;
    private Boolean infoTest;
    private String dbPort;
    /**
     * 密钥
     */
    @Column(name = "`key`")
    private String key;
    private String remark;
    /**
     * 代理ID
     */
    private Long proxyId;
    @CreatedDate
    private Long createdAt;
    @LastModifiedDate
    private Long updatedAt;
}

