package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_session", schema = "it9_data")
public class AiSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "biz_type", nullable = false, length = 30)
    private BizType bizType;

    @Column(name = "biz_id")
    private Long bizId;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "scene_code", nullable = false, length = 100)
    private String sceneCode;

    @Column(name = "session_title", length = 200)
    private String sessionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "memory_mode", nullable = false, length = 20)
    private MemoryMode memoryMode;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "active_model_id")
    private AiModel activeModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private AiPromptTemplate promptTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "default_knowledge_base_id")
    private KnowledgeBase defaultKnowledgeBase;

    @Lob
    @Column(name = "session_summary")
    private String sessionSummary;

    @Column(name = "summary_updated_at")
    private Instant summaryUpdatedAt;

    @Column(name = "ext_config", columnDefinition = "json")
    private String extConfig;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum BizType {
        GENERAL, PROJECT, BLOG, CIRCLE, PAID_CONTENT
    }

    public enum MemoryMode {
        NONE, SHORT, SUMMARY
    }

    public enum Status {
        ACTIVE, ARCHIVED, DELETED
    }
}
