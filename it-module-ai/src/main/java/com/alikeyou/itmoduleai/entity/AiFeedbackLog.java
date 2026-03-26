package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_feedback_log", schema = "it9_data")
public class AiFeedbackLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_log_id")
    private AiCallLog callLog;

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

    public enum FeedbackType {
        LIKE, DISLIKE, ACCEPTED, RETRY
    }
}
