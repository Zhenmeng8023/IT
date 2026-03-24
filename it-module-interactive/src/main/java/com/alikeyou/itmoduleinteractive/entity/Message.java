package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "message", schema = "it_data")
public class Message {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "conversation_id", nullable = false)
    private Long conversationId;

    @Column(name = "sender_id", nullable = false)
    private Long senderId;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @ColumnDefault("'text'")
    @Column(name = "message_type", columnDefinition = "enum('text','image','file','emoji')")
    private String messageType;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "sent_at")
    private Instant sentAt;

    @ColumnDefault("0")
    @Column(name = "is_read")
    private Boolean isRead;
}
