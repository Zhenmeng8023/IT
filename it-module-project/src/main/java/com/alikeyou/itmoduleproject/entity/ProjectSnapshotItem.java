package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_snapshot_item")
public class ProjectSnapshotItem {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "snapshot_id", nullable = false)
    private Long snapshotId;
    @Column(name = "project_file_id", nullable = false)
    private Long projectFileId;
    @Column(name = "project_file_version_id", nullable = false)
    private Long projectFileVersionId;
    @Column(name = "blob_id", nullable = false)
    private Long blobId;
    @Column(name = "canonical_path", nullable = false, length = 1000)
    private String canonicalPath;
    @Column(name = "content_hash", length = 64)
    private String contentHash;
}
