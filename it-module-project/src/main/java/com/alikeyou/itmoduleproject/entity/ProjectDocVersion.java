package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_doc_version")
public class ProjectDocVersion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "doc_id", nullable = false)
    private Long docId;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "title_snapshot", nullable = false, length = 255)
    private String titleSnapshot;

    @Column(name = "doc_type_snapshot", nullable = false, length = 50)
    private String docTypeSnapshot;

    @Column(name = "content_snapshot", nullable = false, columnDefinition = "longtext")
    private String contentSnapshot;

    @Column(name = "content_format_snapshot", length = 30)
    private String contentFormatSnapshot;

    @Column(name = "summary_snapshot", columnDefinition = "text")
    private String summarySnapshot;

    @Column(name = "is_main_readme_snapshot")
    private Boolean isMainReadmeSnapshot;

    @Column(name = "is_pinned_home_snapshot")
    private Boolean isPinnedHomeSnapshot;

    @Column(name = "sort_no_snapshot")
    private Integer sortNoSnapshot;

    @Column(name = "status_snapshot", length = 30)
    private String statusSnapshot;

    @Column(name = "change_summary", columnDefinition = "text")
    private String changeSummary;

    @Column(name = "operator_id")
    private Long operatorId;

    @Column(name = "action_type", length = 30)
    private String actionType;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (actionType == null || actionType.isBlank()) {
            actionType = "save";
        }
        if (contentFormatSnapshot == null || contentFormatSnapshot.isBlank()) {
            contentFormatSnapshot = "markdown";
        }
        if (statusSnapshot == null || statusSnapshot.isBlank()) {
            statusSnapshot = "published";
        }
        if (docTypeSnapshot == null || docTypeSnapshot.isBlank()) {
            docTypeSnapshot = "readme";
        }
        if (isMainReadmeSnapshot == null) {
            isMainReadmeSnapshot = false;
        }
        if (isPinnedHomeSnapshot == null) {
            isPinnedHomeSnapshot = false;
        }
        if (sortNoSnapshot == null) {
            sortNoSnapshot = 0;
        }
    }
}
