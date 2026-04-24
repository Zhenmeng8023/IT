package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "knowledge_base", schema = "it9_data")
public class KnowledgeBase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 30)
    private ScopeType scopeType;

    @Column(name = "project_id")
    private Long projectId;

    @Column(name = "owner_id")
    private Long ownerId;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Enumerated(EnumType.STRING)
    @Column(name = "chunk_strategy", nullable = false, length = 30)
    private ChunkStrategy chunkStrategy;

    @Column(name = "default_top_k")
    private Integer defaultTopK;

    @Enumerated(EnumType.STRING)
    @Column(name = "visibility", nullable = false, length = 20)
    private Visibility visibility;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "doc_count")
    private Integer docCount;

    @Column(name = "chunk_count")
    private Integer chunkCount;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum ScopeType {
        PERSONAL, PROJECT, PLATFORM
    }

    public enum SourceType {
        MANUAL, UPLOAD, PROJECT_DOC, BLOG, CIRCLE, PAID_CONTENT, MIXED
    }

    public enum ChunkStrategy {
        FIXED, PARAGRAPH, MARKDOWN, CUSTOM
    }

    public enum Visibility {
        PRIVATE, TEAM, PUBLIC
    }

    public enum Status {
        DRAFT, INDEXING, ACTIVE, DISABLED, ARCHIVED, FAILED
    }
}
