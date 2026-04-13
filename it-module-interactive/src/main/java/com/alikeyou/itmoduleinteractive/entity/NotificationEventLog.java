package com.alikeyou.itmoduleinteractive.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "notification_event_log", schema = "it9_data")
public class NotificationEventLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "event_key", nullable = false, length = 191)
    private String eventKey;

    @Column(name = "category", nullable = false, length = 50)
    private String category;

    @Column(name = "type", nullable = false, length = 50)
    private String type;

    @Column(name = "sender_id")
    private Long senderId;

    @Column(name = "source_type", length = 80)
    private String sourceType;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "target_type", length = 80)
    private String targetType;

    @Column(name = "target_id")
    private Long targetId;

    @ColumnDefault("0")
    @Column(name = "receiver_count", nullable = false)
    private Integer receiverCount;

    @ColumnDefault("'created'")
    @Column(name = "status", nullable = false, length = 30)
    private String status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "payload_json", columnDefinition = "json")
    private String payloadJson;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
