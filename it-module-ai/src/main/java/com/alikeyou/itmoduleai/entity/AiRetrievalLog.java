package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_retrieval_log", schema = "it9_data")
public class AiRetrievalLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "call_log_id", nullable = false)
    private AiCallLog callLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document;

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

    public enum RetrievalMethod {
        VECTOR, KEYWORD, HYBRID, MANUAL
    }
}
