package com.alikeyou.itmoduleproject.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Data @Builder @NoArgsConstructor @AllArgsConstructor
@Entity
@Table(name = "project_blob")
public class ProjectBlob {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "sha256", nullable = false, length = 64)
    private String sha256;
    @Column(name = "size_bytes", nullable = false)
    private Long sizeBytes;
    @Column(name = "mime_type", length = 100)
    private String mimeType;
    @Column(name = "storage_path", nullable = false, length = 1000)
    private String storagePath;
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    @PrePersist public void prePersist() { if (createdAt == null) createdAt = LocalDateTime.now(); }
}
