package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_message", schema = "it9_data")
public class AiMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private AiSession session;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private Role role;

    @Column(name = "sender_user_id")
    private Long senderUserId;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "content_tokens")
    private Integer contentTokens;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prompt_template_id")
    private AiPromptTemplate promptTemplate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id")
    private AiModel model;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @Column(name = "quoted_chunk_ids", columnDefinition = "json")
    private String quotedChunkIds;

    @Column(name = "tool_call_json", columnDefinition = "json")
    private String toolCallJson;

    @Column(name = "latency_ms")
    private Integer latencyMs;

    @Column(name = "finish_reason", length = 50)
    private String finishReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at")
    private Instant createdAt;

    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    public enum Status {
        SUCCESS, FAILED, CANCELLED
    }
}
