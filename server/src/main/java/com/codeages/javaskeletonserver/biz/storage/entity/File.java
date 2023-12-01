package com.codeages.javaskeletonserver.biz.storage.entity;

import com.codeages.javaskeletonserver.biz.storage.enums.FileTargetTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

@Entity
@Data
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class File {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String ext;
    private Long size;
    private Long userId;
    private String uri;
    private String uuid;
    private Date uploadTime;
    @Enumerated(EnumType.STRING)
    private FileTargetTypeEnum targetType;
    @CreatedDate
    private Long createdAt;

    @LastModifiedDate
    private Long updatedAt;
}
