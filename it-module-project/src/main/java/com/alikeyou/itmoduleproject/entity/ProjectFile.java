package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_file")
public class ProjectFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(name = "repository_id")
    private Long repositoryId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "canonical_path", length = 1000)
    private String canonicalPath;

    @Column(name = "canonical_path_hash", length = 64, insertable = false, updatable = false)
    private String canonicalPathHash;

    @Column(name = "file_key", length = 1100)
    private String fileKey;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    @Column(name = "is_main")
    private Boolean isMain;

    @Column(length = 20)
    private String version;

    @Column(name = "latest_blob_id")
    private Long latestBlobId;

    @Column(name = "latest_version_id")
    private Long latestVersionId;

    @Column(name = "latest_commit_id")
    private Long latestCommitId;

    @Column(name = "is_latest")
    private Boolean isLatest;

    @Column(name = "deleted_flag")
    private Boolean deletedFlag;

    @Column(name = "content_hash", length = 64)
    private String contentHash;

    @Column(name = "last_modified_at")
    private LocalDateTime lastModifiedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (uploadTime == null) uploadTime = now;
        if (isMain == null) isMain = false;
        if (isLatest == null) isLatest = true;
        if (deletedFlag == null) deletedFlag = false;
        if (lastModifiedAt == null) lastModifiedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        lastModifiedAt = LocalDateTime.now();
    }
}
