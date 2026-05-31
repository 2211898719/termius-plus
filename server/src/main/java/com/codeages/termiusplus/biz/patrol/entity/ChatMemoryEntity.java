package com.codeages.termiusplus.biz.patrol.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "`patrol_chat_memory`")
public class ChatMemoryEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36, unique = true)
    private String conversationId;

    @Column(columnDefinition = "MEDIUMTEXT")
    private String messagesJson;

    @CreatedDate
    private Long createdAt;
}
