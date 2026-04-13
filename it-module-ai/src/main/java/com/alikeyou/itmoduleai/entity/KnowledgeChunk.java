package com.alikeyou.itmoduleai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "knowledge_chunk", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgeChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "document_id", nullable = false)
    private KnowledgeDocument document;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_type", nullable = false, length = 30)
    private ChunkType chunkType;

    @Lob
    @Column(name = "content", nullable = false)
    private String content;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "vector_id", length = 255)
    private String vectorId;

    @Column(name = "file_path", length = 1000)
    private String filePath;

    @Column(name = "file_path_hash", length = 64, insertable = false, updatable = false)
    private String filePathHash;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "start_column")
    private Integer startColumn;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "end_column")
    private Integer endColumn;

    @Column(name = "section_path", length = 500)
    private String sectionPath;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "primary_symbol_id")
    private AiCodeSymbol primarySymbol;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "rank_boost", precision = 8, scale = 4)
    private BigDecimal rankBoost;

    @Column(name = "retrieval_tags", columnDefinition = "json")
    private String retrievalTags;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        if (chunkType == null) {
            chunkType = ChunkType.TEXT;
        }
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBase != null ? knowledgeBase.getId() : null;
    }

    public Long getDocumentId() {
        return document != null ? document.getId() : null;
    }

    public Long getPrimarySymbolId() {
        return primarySymbol != null ? primarySymbol.getId() : null;
    }

    public enum ChunkType {
        TEXT,
        CODE_BLOCK,
        SYMBOL_DECL,
        SYMBOL_BODY
    }
}
