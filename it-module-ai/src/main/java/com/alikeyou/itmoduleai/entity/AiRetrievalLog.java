package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_retrieval_log", schema = "it_data")
public class AiRetrievalLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "call_log_id", nullable = false)
    private AiCallLog callLog;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "knowledge_base_id")
    private KnowledgeBase knowledgeBase;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "document_id")
    private KnowledgeDocument document;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "chunk_id")
    private KnowledgeChunk chunk;

    @Lob
    @Column(name = "query_text")
    private String queryText;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "retrieval_method", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private RetrievalMethod retrievalMethod;

    public enum RetrievalMethod {
        VECTOR, KEYWORD, HYBRID, MANUAL
    }

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;
}
