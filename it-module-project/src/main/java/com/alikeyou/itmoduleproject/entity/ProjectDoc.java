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

    @Column(name = "doc_type", nullable = false, length = 30)
    private String docType;

    @Column(nullable = false, length = 20)
    private String status;

    @Column(nullable = false, length = 20)
    private String visibility;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "current_content", columnDefinition = "longtext")
    private String currentContent;

    @Column(name = "current_version", nullable = false)
    private Integer currentVersion;

    @Column(name = "creator_id")
    private Long creatorId;

    @Column(name = "editor_id")
    private Long editorId;

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
        if (currentVersion == null || currentVersion < 1) {
            currentVersion = 1;
        }
        if (docType == null || docType.isBlank()) {
            docType = "wiki";
        }
        if (status == null || status.isBlank()) {
            status = "draft";
        }
        if (visibility == null || visibility.isBlank()) {
            visibility = "project";
        }
        if (isPrimary == null) {
            isPrimary = Boolean.FALSE;
        }
        if (currentContent == null) {
            currentContent = "";
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (currentVersion == null || currentVersion < 1) {
            currentVersion = 1;
        }
        if (docType == null || docType.isBlank()) {
            docType = "wiki";
        }
        if (status == null || status.isBlank()) {
            status = "draft";
        }
        if (visibility == null || visibility.isBlank()) {
            visibility = "project";
        }
        if (isPrimary == null) {
            isPrimary = Boolean.FALSE;
        }
        if (currentContent == null) {
            currentContent = "";
        }
    }
}
