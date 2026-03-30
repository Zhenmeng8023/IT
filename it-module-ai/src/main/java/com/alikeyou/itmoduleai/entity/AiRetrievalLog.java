package com.alikeyou.itmoduleai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.Supplier;

@Getter
@Setter
@Entity
@Table(name = "ai_retrieval_log", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiRetrievalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_log_id", nullable = false)
    private AiCallLog callLog;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private KnowledgeChunk chunk;

    @Lob
    @Column(name = "query_text")
    private String queryText;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_method", nullable = false, length = 20)
    private RetrievalMethod retrievalMethod;

    @Column(name = "created_at")
    private Instant createdAt;

    public Long getCallLogId() {
        return safe(() -> callLog.getId());
    }

    public Long getKnowledgeBaseId() {
        return safe(() -> knowledgeBase.getId());
    }

    public String getKnowledgeBaseName() {
        return safe(() -> knowledgeBase.getName());
    }

    public Long getDocumentId() {
        return safe(() -> document.getId());
    }

    public String getDocumentTitle() {
        return safe(() -> document.getTitle());
    }

    public Long getChunkId() {
        return safe(() -> chunk.getId());
    }

    private <T> T safe(Supplier<T> supplier) {
        try {
            return supplier == null ? null : supplier.get();
        } catch (Exception e) {
            return null;
        }
    }

    public enum RetrievalMethod {
        VECTOR,
        KEYWORD,
        HYBRID,
        MANUAL
    }
}
