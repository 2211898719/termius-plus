package com.codeages.termiusplus.biz.objectlog.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
public class ObjectLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;

    private String type;

    private Long oid = 0L;

    private String event;

    private String message;

    private String context;

    private Long operatorId = 0L;

    @CreatedDate
    private Long createdAt;
}
