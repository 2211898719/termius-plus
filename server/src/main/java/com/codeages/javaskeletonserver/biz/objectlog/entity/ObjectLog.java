package com.codeages.javaskeletonserver.biz.objectlog.entity;

import com.vladmihalcea.hibernate.type.json.JsonStringType;
import lombok.Data;
import org.hibernate.annotations.Type;
import org.hibernate.annotations.TypeDef;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Map;

@Entity
@EntityListeners(AuditingEntityListener.class)
@Data
@TypeDef(name = "json", typeClass = JsonStringType.class)
public class ObjectLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String level;

    private String type;

    private Long oid = 0L;

    private String event;

    private String message;

    @Type(type = "json")
    private Map<String, Object> context;

    private Long operatorId = 0L;

    @CreatedDate
    private Long createdAt;
}
