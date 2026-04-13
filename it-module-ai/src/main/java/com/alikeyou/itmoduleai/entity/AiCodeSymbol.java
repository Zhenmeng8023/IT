package com.alikeyou.itmoduleai.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_code_symbol", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class AiCodeSymbol {

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

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chunk_id")
    private KnowledgeChunk chunk;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "branch_id")
    private Long branchId;

    @Column(name = "commit_id")
    private Long commitId;

    @Column(name = "project_file_id")
    private Long projectFileId;

    @Column(name = "project_file_version_id")
    private Long projectFileVersionId;

    @Column(name = "language", length = 30)
    private String language;

    @Column(name = "file_path", nullable = false, length = 1000)
    private String filePath;

    @Column(name = "file_path_hash", length = 64, insertable = false, updatable = false)
    private String filePathHash;

    @Column(name = "symbol_name", nullable = false, length = 255)
    private String symbolName;

    @Column(name = "qualified_name", nullable = false, length = 1000)
    private String qualifiedName;

    @Column(name = "qualified_name_hash", length = 64, insertable = false, updatable = false)
    private String qualifiedNameHash;

    @Column(name = "symbol_kind", nullable = false, length = 40)
    private String symbolKind;

    @Column(name = "symbol_scope", length = 40)
    private String symbolScope;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_symbol_id")
    private AiCodeSymbol parentSymbol;

    @Lob
    @Column(name = "signature")
    private String signature;

    @Column(name = "return_type", length = 255)
    private String returnType;

    @Column(name = "visibility", length = 30)
    private String visibility;

    @Column(name = "modifiers_json", columnDefinition = "json")
    private String modifiersJson;

    @Column(name = "start_line", nullable = false)
    private Integer startLine;

    @Column(name = "start_column")
    private Integer startColumn;

    @Column(name = "end_line")
    private Integer endLine;

    @Column(name = "end_column")
    private Integer endColumn;

    @Column(name = "body_start_line")
    private Integer bodyStartLine;

    @Column(name = "body_end_line")
    private Integer bodyEndLine;

    @Column(name = "is_declaration", nullable = false)
    private Boolean isDeclaration;

    @Column(name = "is_exported")
    private Boolean isExported;

    @Lob
    @Column(name = "doc_comment")
    private String docComment;

    @Column(name = "symbol_key", length = 128)
    private String symbolKey;

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
        if (startLine == null) {
            startLine = 1;
        }
        if (isDeclaration == null) {
            isDeclaration = Boolean.TRUE;
        }
        if (status == null) {
            status = Status.ACTIVE;
        }
    }

    public Long getKnowledgeBaseId() {
        return knowledgeBase == null ? null : knowledgeBase.getId();
    }

    public Long getDocumentId() {
        return document == null ? null : document.getId();
    }

    public Long getChunkId() {
        return chunk == null ? null : chunk.getId();
    }

    public Long getParentSymbolId() {
        return parentSymbol == null ? null : parentSymbol.getId();
    }

    public enum Status {
        ACTIVE,
        STALE,
        DELETED
    }
}
