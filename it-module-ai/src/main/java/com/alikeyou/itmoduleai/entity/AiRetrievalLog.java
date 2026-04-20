package com.alikeyou.itmoduleai.entity;

import com.alikeyou.itmoduleai.enums.GroundingStatus;
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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private AiSession session;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "message_id")
    private AiMessage message;

    @Enumerated(EnumType.STRING)
    @Column(name = "stage_code", nullable = false, length = 30)
    private StageCode stageCode;

    @Column(name = "phase", length = 50)
    private String phase;

    @Column(name = "stage_order")
    private Integer stageOrder;

    @Lob
    @Column(name = "query_variant")
    private String queryVariant;

    @Column(name = "plan_step_json", columnDefinition = "json")
    private String planStepJson;

    @Column(name = "hit_reason_json", columnDefinition = "json")
    private String hitReasonJson;

    @Column(name = "score_detail_json", columnDefinition = "json")
    private String scoreDetailJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "candidate_source", length = 30)
    private CandidateSource candidateSource;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "symbol_id")
    private AiCodeSymbol symbol;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reference_id")
    private AiCodeReference reference;

    @Lob
    @Column(name = "query_text")
    private String queryText;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "score_vector", precision = 10, scale = 6)
    private BigDecimal scoreVector;

    @Column(name = "score_keyword", precision = 10, scale = 6)
    private BigDecimal scoreKeyword;

    @Column(name = "score_graph", precision = 10, scale = 6)
    private BigDecimal scoreGraph;

    @Column(name = "score_rerank", precision = 10, scale = 6)
    private BigDecimal scoreRerank;

    @Column(name = "rerank_model", length = 100)
    private String rerankModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "grounding_status", length = 30)
    private GroundingStatus groundingStatus;

    @Column(name = "grounding_evidence_json", columnDefinition = "json")
    private String groundingEvidenceJson;

    @Column(name = "degrade_reason", length = 500)
    private String degradeReason;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Enumerated(EnumType.STRING)
    @Column(name = "retrieval_method", nullable = false, length = 20)
    private RetrievalMethod retrievalMethod;

    @Column(name = "created_at")
    private Instant createdAt;

    @PrePersist
    private void onCreate() {
        if (stageCode == null) {
            stageCode = StageCode.RECALL;
        }
    }

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

    public String getChunkEmbeddingProvider() {
        return safe(() -> chunk.getEmbeddingProvider());
    }

    public String getChunkEmbeddingModel() {
        return safe(() -> chunk.getEmbeddingModel());
    }

    public String getChunkVectorId() {
        return safe(() -> chunk.getVectorId());
    }

    public Long getSessionId() {
        return safe(() -> session.getId());
    }

    public Long getMessageId() {
        return safe(() -> message.getId());
    }

    public Long getSymbolId() {
        return safe(() -> symbol.getId());
    }

    public Long getReferenceId() {
        return safe(() -> reference.getId());
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

    public enum StageCode {
        PLAN,
        RECALL,
        DECLARATION_FIRST,
        GRAPH_EXPAND,
        RERANK,
        GROUND
    }

    public enum CandidateSource {
        CHUNK,
        DOCUMENT,
        SYMBOL,
        REFERENCE
    }
}
