package com.alikeyou.itmoduleai.entity;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@Entity
@Table(name = "knowledge_base", schema = "it_data")
public class KnowledgeBase {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Lob
    @Column(name = "description")
    private String description;

    @Column(name = "scope_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ScopeType scopeType;

    public enum ScopeType {
        PERSONAL, PROJECT, PLATFORM
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.SET_NULL)
    @JoinColumn(name = "owner_id")
    private UserInfo owner;

    // Deleted:@ManyToOne(fetch = FetchType.LAZY)
    // Deleted:@OnDelete(action = OnDeleteAction.CASCADE)
    // Deleted:@JoinColumn(name = "project_id")
    // Deleted:private com.alikeyou.itmoduleblog.entity.Project project;

    @Column(name = "source_type", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private SourceType sourceType;

    public enum SourceType {
        MANUAL, UPLOAD, PROJECT_FILE, PROJECT_DOC, BLOG, MIXED
    }

    @Column(name = "embedding_provider", length = 50)
    private String embeddingProvider;

    @Column(name = "embedding_model", length = 100)
    private String embeddingModel;

    @Column(name = "chunk_strategy", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private ChunkStrategy chunkStrategy;

    public enum ChunkStrategy {
        FIXED, PARAGRAPH, MARKDOWN, CUSTOM
    }

    @ColumnDefault("5")
    @Column(name = "default_top_k", nullable = false)
    private Integer defaultTopK;

    @Column(name = "visibility", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Visibility visibility;

    public enum Visibility {
        PRIVATE, TEAM, PUBLIC
    }

    @Column(name = "status", nullable = false, length = 50)
    @Enumerated(EnumType.STRING)
    private Status status;

    public enum Status {
        DRAFT, INDEXING, ACTIVE, DISABLED, FAILED
    }

    @ColumnDefault("0")
    @Column(name = "doc_count", nullable = false)
    private Integer docCount;

    @ColumnDefault("0")
    @Column(name = "chunk_count", nullable = false)
    private Integer chunkCount;

    @Column(name = "last_indexed_at")
    private Instant lastIndexedAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "created_at")
    private Instant createdAt;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "updated_at")
    private Instant updatedAt;
}
