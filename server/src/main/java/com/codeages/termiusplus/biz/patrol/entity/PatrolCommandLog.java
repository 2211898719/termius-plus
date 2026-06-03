package com.codeages.termiusplus.biz.patrol.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "`patrol_command_log`")
public class PatrolCommandLog {

    public static final String TYPE_AUTO = "AUTO";
    public static final String TYPE_CONFIRM_PENDING = "CONFIRM_PENDING";
    public static final String TYPE_DANGEROUS = "DANGEROUS";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long serverId;

    @Column(columnDefinition = "TEXT")
    private String command;

    @Column(nullable = false, length = 20)
    private String execType;

    @Column(columnDefinition = "TEXT")
    private String output;

    @CreatedDate
    private Long createdAt;
}
