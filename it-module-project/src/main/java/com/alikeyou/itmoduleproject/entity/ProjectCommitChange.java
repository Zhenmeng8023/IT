package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_commit_change")
public class ProjectCommitChange {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "commit_id", nullable = false)
    private Long commitId;
    @Column(name = "project_file_id")
    private Long projectFileId;
    @Column(name = "old_blob_id")
    private Long oldBlobId;
    @Column(name = "new_blob_id")
    private Long newBlobId;
    @Column(name = "old_path", length = 1000)
    private String oldPath;
    @Column(name = "new_path", length = 1000)
    private String newPath;
    @Column(name = "change_type", nullable = false, length = 20)
    private String changeType;
    @Column(name = "diff_summary_json", columnDefinition = "json")
    private String diffSummaryJson;
}
