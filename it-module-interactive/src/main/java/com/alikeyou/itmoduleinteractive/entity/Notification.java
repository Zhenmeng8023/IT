package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification", schema = "it9_data")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "receiver_id", nullable = false)
    private Long receiverId;

    @Column(name = "sender_id")
    private Long senderId;

    @ColumnDefault("'system'")
    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @ColumnDefault("''")
    @Column(name = "title", nullable = false, length = 120)
    private String title;

    @Column(name = "content", nullable = false)
    private String content;

    @ColumnDefault("0")
    @Column(name = "read_status")
    private Boolean readStatus;

    @Column(name = "read_at")
    private Instant readAt;

    @Column(name = "target_type", length = 80)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "event_key", length = 191)
    private String eventKey;

    @Column(name = "action_url", length = 500)
    private String actionUrl;

    @ColumnDefault("'open'")
    @Column(name = "business_status", nullable = false, length = 30)
    private String businessStatus;

    @ColumnDefault("0")
    @Column(name = "priority", nullable = false)
    private Integer priority;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "deleted_at")
    private Instant deletedAt;

    @Transient
    private String senderName;

    @Transient
    private String senderAvatar;

    @Transient
    private String targetTitle;

    @Transient
    private String preview;

    @Transient
    private String actionText;

    @Transient
    private Long blogId;

    @Transient
    private Long commentId;

}
