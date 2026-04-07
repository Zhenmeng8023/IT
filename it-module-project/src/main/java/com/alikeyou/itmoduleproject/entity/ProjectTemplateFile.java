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
@Table(name = "project_template_file")
public class ProjectTemplateFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false)
    private Long templateId;

    @Column(name = "item_type", nullable = false, length = 30)
    private String itemType;

    @Column(name = "item_key", length = 100)
    private String itemKey;

    @Column(name = "group_name", length = 30)
    private String groupName;

    @Column(name = "source_id")
    private Long sourceId;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_ext", length = 50)
    private String fileExt;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "include_content")
    private Boolean includeContent;

    @Column(length = 20)
    private String version;

    @Column(name = "sort_order")
    private Integer sortOrder;

    @Column(name = "payload_json", columnDefinition = "longtext")
    private String payloadJson;

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
        if (version == null || version.isBlank()) {
            version = "1.0";
        }
        if (itemType == null || itemType.isBlank()) {
            itemType = "file";
        }
        if (groupName == null || groupName.isBlank()) {
            groupName = itemType;
        }
        if (includeContent == null) {
            includeContent = Boolean.FALSE;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = LocalDateTime.now();
        if (version == null || version.isBlank()) {
            version = "1.0";
        }
        if (itemType == null || itemType.isBlank()) {
            itemType = "file";
        }
        if (groupName == null || groupName.isBlank()) {
            groupName = itemType;
        }
        if (includeContent == null) {
            includeContent = Boolean.FALSE;
        }
        if (sortOrder == null) {
            sortOrder = 0;
        }
    }
}
