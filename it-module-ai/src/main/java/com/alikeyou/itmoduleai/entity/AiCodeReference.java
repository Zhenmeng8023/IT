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
@Table(name = "ai_code_reference", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiCodeReference {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "commit_id")
    private Long commitId;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_symbol_id")
    private AiCodeSymbol fromSymbol;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_symbol_id")
    private AiCodeSymbol toSymbol;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "from_document_id", nullable = false)
    private KnowledgeDocument fromDocument;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_chunk_id")
    private KnowledgeChunk fromChunk;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_document_id")
    private KnowledgeDocument toDocument;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_chunk_id")
    private KnowledgeChunk toChunk;

    @Column(name = "ref_kind", nullable = false, length = 40)
    private String refKind;

    @Column(name = "ref_name", length = 255)
    private String refName;

    @Column(name = "target_qualified_name", length = 1000)
    private String targetQualifiedName;

    @Column(name = "target_qualified_name_hash", length = 64, insertable = false, updatable = false)
    private String targetQualifiedNameHash;

    @Column(name = "from_file_path", length = 1000)
    private String fromFilePath;

    @Column(name = "from_file_path_hash", length = 64, insertable = false, updatable = false)
    private String fromFilePathHash;

    @Column(name = "start_line")
    private Integer startLine;

    @Column(name = "start_column")
    private Integer startColumn;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "end_column")
    private Integer endColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_status", nullable = false, length = 30)
    private ResolutionStatus resolutionStatus;

    @Column(name = "confidence", precision = 8, scale = 6)
    private BigDecimal confidence;

    @Column(name = "reference_key", length = 128)
    private String referenceKey;

    @Column(name = "metadata_json", columnDefinition = "json")
    private String metadataJson;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @PrePersist
    private void onCreate() {
        if (resolutionStatus == null) {
            resolutionStatus = ResolutionStatus.UNRESOLVED;
        }
        if (status == null) {
            status = Status.ACTIVE;
        }
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBase == null ? null : knowledgeBase.getId();
    }

    public Long getFromSymbolId() {
        return fromSymbol == null ? null : fromSymbol.getId();
    }

    public Long getToSymbolId() {
        return toSymbol == null ? null : toSymbol.getId();
    }

    public Long getFromDocumentId() {
        return fromDocument == null ? null : fromDocument.getId();
    }

    public Long getFromChunkId() {
        return fromChunk == null ? null : fromChunk.getId();
    }

    public Long getToDocumentId() {
        return toDocument == null ? null : toDocument.getId();
    }

    public Long getToChunkId() {
        return toChunk == null ? null : toChunk.getId();
    }

    public enum ResolutionStatus {
        RESOLVED,
        UNRESOLVED,
        AMBIGUOUS,
        EXTERNAL
    }

    public enum Status {
        ACTIVE,
        STALE,
        DELETED
    }
}
