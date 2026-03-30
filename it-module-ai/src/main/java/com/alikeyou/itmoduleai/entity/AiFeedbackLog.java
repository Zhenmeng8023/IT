package com.alikeyou.itmoduleai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.function.Supplier;

@Getter
@Setter
@Entity
@Table(name = "ai_feedback_log", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiFeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_log_id")
    private AiCallLog callLog;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private AiMessage message;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "feedback_type", nullable = false, length = 20)
    private FeedbackType feedbackType;

    @Column(name = "comment_text", length = 500)
    private String commentText;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getCallLogId() {
        return safe(() -> callLog.getId());
    }

    public Long getMessageId() {
        return safe(() -> message.getId());
    }

    private <T> T safe(Supplier<T> supplier) {
        try {
            return supplier == null ? null : supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    public enum FeedbackType {
        LIKE,
        DISLIKE,
        ACCEPTED,
        RETRY
    }
}
