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
@Table(name = "knowledge_import_task", schema = "it9_data")
@JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
public class KnowledgeImportTask {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "knowledge_base_id", nullable = false)
    private KnowledgeBase knowledgeBase;

    @Column(name = "zip_name", nullable = false, length = 255)
    private String zipName;

    @JsonIgnore
    @Column(name = "temp_zip_path", nullable = false, length = 1000)
    private String tempZipPath;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_stage", nullable = false, length = 30)
    private Stage currentStage;

    @Column(name = "total_files")
    private Integer totalFiles;

    @Column(name = "scanned_files")
    private Integer scannedFiles;

    @Column(name = "imported_files")
    private Integer importedFiles;

    @Column(name = "skipped_files")
    private Integer skippedFiles;

    @Column(name = "failed_files")
    private Integer failedFiles;

    @Column(name = "progress_percent")
    private Integer progressPercent;

    @Column(name = "current_file", length = 1000)
    private String currentFile;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "cancel_requested")
    private Boolean cancelRequested;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    public Long getKnowledgeBaseId() {
        return knowledgeBase != null ? knowledgeBase.getId() : null;
    }

    public enum Status {
        PENDING,
        RUNNING,
        SUCCESS,
        FAILED,
        CANCELLED
    }

    public enum Stage {
        UPLOADED,
        SCANNING,
        IMPORTING,
        FINISHED,
        CANCELLED
    }
}
