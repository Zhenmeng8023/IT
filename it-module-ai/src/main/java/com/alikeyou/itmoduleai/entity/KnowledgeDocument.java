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
@Table(name = "knowledge_document", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 30)
    private SourceType sourceType;

    @Column(name = "source_ref_id")
    private Long sourceRefId;

    @Column(name = "source_url", length = 500)
    private String sourceUrl;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "file_name", length = 255)
    private String fileName;

    @Column(name = "archive_name", length = 255)
    private String archiveName;

    @Column(name = "archive_entry_path", length = 1000)
    private String archiveEntryPath;

    @Column(name = "import_batch_id", length = 64)
    private String importBatchId;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "storage_path", length = 500)
    private String storagePath;

    @Lob
    @Column(name = "content_text")
    private String contentText;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "language", length = 20)
    private String language;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "indexed_at")
    private Instant indexedAt;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public Long getKnowledgeBaseId() {
        return knowledgeBase != null ? knowledgeBase.getId() : null;
    }

    public boolean hasStoredFile() {
        return storagePath != null && !storagePath.isBlank();
    }

    public boolean isImportedFromZip() {
        return archiveName != null && !archiveName.isBlank();
    }

    public enum SourceType {
        MANUAL,
        UPLOAD,
        PROJECT_DOC,
        BLOG,
        CIRCLE,
        PAID_CONTENT,
        URL
    }

    public enum Status {
        UPLOADED,
        PARSING,
        INDEXED,
        FAILED,
        DISABLED
    }
}