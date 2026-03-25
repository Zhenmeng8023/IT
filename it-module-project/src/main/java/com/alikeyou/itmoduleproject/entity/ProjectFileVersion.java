/**
 * 项目文件版本实体类
 * 对应数据库中的project_file_version表，存储项目文件的版本信息
 */
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

/**
 * 项目文件版本实体类
 * 对应数据库中的project_file_version表，存储项目文件的版本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_file_version")
public class ProjectFileVersion {

    /**
     * 版本ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 文件ID，不能为空
     */
    @Column(name = "file_id", nullable = false)
    private Long fileId;

    /**
     * 版本号，不能为空，最大长度20
     */
    @Column(nullable = false, length = 20)
    private String version;

    /**
     * 服务器路径，不能为空，最大长度500
     */
    @Column(name = "server_path", nullable = false, length = 500)
    private String serverPath;

    /**
     * 文件大小（字节）
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * 上传者ID
     */
    @Column(name = "uploaded_by")
    private Long uploadedBy;

    /**
     * 提交信息
     */
    @Column(name = "commit_message", columnDefinition = "text")
    private String commitMessage;

    /**
     * 上传时间
     */
    @Column(name = "uploaded_at")
    private LocalDateTime uploadedAt;

    /**
     * 持久化前的处理
     * 设置默认上传时间
     */
    @PrePersist
    public void prePersist() {
        if (uploadedAt == null) {
            uploadedAt = LocalDateTime.now();
        }
    }
}