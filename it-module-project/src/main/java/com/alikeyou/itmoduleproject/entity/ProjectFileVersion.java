package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_file_version")
public class ProjectFileVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "file_id", nullable = false)
    private Long fileId;
    @Column(name = "repository_id")
    private Long repositoryId;
    @Column(nullable = false, length = 20)
    private String version;
    @Column(name = "commit_id")
    private Long commitId;
    @Column(name = "blob_id")
    private Long blobId;
    @Column(name = "version_seq")
    private Integer versionSeq;
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    @Column(name = "path_at_version", length = 1000)
    private String pathAtVersion;
    @Column(name = "change_type", length = 20)
    private String changeType;
    @Column(name = "parent_version_id")
    private Long parentVersionId;
    @Column(name = "server_path", nullable = false, length = 500)
    private String serverPath;
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;
    @Column(name = "uploaded_by")
    private Long uploadedBy;
    @Column(name = "commit_message", columnDefinition = "text")
    private String commitMessage;
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;
    @PrePersist
    public void prePersist() { if (uploadedAt == null) uploadedAt = LocalDateTime.now(); }
}
