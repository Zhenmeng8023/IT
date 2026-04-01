package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
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
@Table(name = "project_doc")
public class ProjectDoc {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_id", nullable = false)
    private Long projectId;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(name = "doc_type", nullable = false, length = 50)
    private String docType;

    @Column(nullable = false, columnDefinition = "longtext")
    private String content;

    @Column(name = "content_format", length = 30)
    private String contentFormat;

    @Column(columnDefinition = "text")
    private String summary;

    @Column(name = "is_main_readme")
    private Boolean isMainReadme;

    @Column(name = "is_pinned_home")
    private Boolean isPinnedHome;

    @Column(name = "sort_no")
    private Integer sortNo;

    @Column(length = 30)
    private String status;

    @Column(name = "latest_version_no")
    private Integer latestVersionNo;

    @Column(name = "created_by")
    private Long createdBy;

    @Column(name = "updated_by")
    private Long updatedBy;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (isMainReadme == null) {
            isMainReadme = false;
        }
        if (isPinnedHome == null) {
            isPinnedHome = false;
        }
        if (sortNo == null) {
            sortNo = 0;
        }
        if (latestVersionNo == null) {
            latestVersionNo = 1;
        }
        if (contentFormat == null || contentFormat.isBlank()) {
            contentFormat = "markdown";
        }
        if (status == null || status.isBlank()) {
            status = "published";
        }
        if (docType == null || docType.isBlank()) {
            docType = "readme";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (contentFormat == null || contentFormat.isBlank()) {
            contentFormat = "markdown";
        }
        if (status == null || status.isBlank()) {
            status = "published";
        }
        if (docType == null || docType.isBlank()) {
            docType = "readme";
        }
        if (sortNo == null) {
            sortNo = 0;
        }
        if (isMainReadme == null) {
            isMainReadme = false;
        }
        if (isPinnedHome == null) {
            isPinnedHome = false;
        }
    }
}
