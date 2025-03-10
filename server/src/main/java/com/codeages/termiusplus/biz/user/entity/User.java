package com.codeages.termiusplus.biz.user.entity;

import com.codeages.termiusplus.common.cache.IdObject;
import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class User implements IdObject {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String username;

    private String password;

    private Boolean locked = false;

    private String email = "";

    private String avatar = "";

    private Long registerAt;

    private String registerIp;

    private Long loginAt = 0L;

    private String loginIp = "";

    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;
}
