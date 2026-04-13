package com.alikeyou.itmoduleai.entity;

import com.alikeyou.itmoduleai.enums.GroundingStatus;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_message_id")
    private AiMessage parentMessage;

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

    @Column(name = "turn_index")
    private Integer turnIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_format", nullable = false, length = 30)
    private ContentFormat contentFormat;

    @Enumerated(EnumType.STRING)
    @Column(name = "stream_state", nullable = false, length = 20)
    private StreamState streamState;

    @Column(name = "partial_seq", nullable = false)
    private Integer partialSeq;

    @Lob
    @Column(name = "delta_content")
    private String deltaContent;

    @Column(name = "retrieval_summary_json", columnDefinition = "json")
    private String retrievalSummaryJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "grounding_status", length = 30)
    private GroundingStatus groundingStatus;

    @Column(name = "grounding_json", columnDefinition = "json")
    private String groundingJson;

    @Column(name = "citation_json", columnDefinition = "json")
    private String citationJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        if (contentFormat == null) {
            contentFormat = ContentFormat.TEXT;
        }
        if (streamState == null) {
            streamState = StreamState.FINAL;
        }
        if (partialSeq == null) {
            partialSeq = 0;
        }
    }

    @PreUpdate
    private void onUpdate() {
        updatedAt = Instant.now();
    }

    public Long getParentMessageId() {
        return parentMessage == null ? null : parentMessage.getId();
    }

    public enum Role {
        SYSTEM, USER, ASSISTANT, TOOL
    }

    public enum ContentFormat {
        TEXT, MARKDOWN, JSON, TOOL_RESULT
    }

    public enum StreamState {
        FINAL, PARTIAL, DELTA, CANCELLED
    }

    public enum Status {
        SUCCESS, FAILED, CANCELLED, INTERRUPTED
    }
}
