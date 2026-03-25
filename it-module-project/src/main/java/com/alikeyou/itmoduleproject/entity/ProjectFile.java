/**
 * 项目文件实体类
 * 对应数据库中的project_file表，存储项目文件的基本信息
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
 * 项目文件实体类
 * 对应数据库中的project_file表，存储项目文件的基本信息
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "project_file")
public class ProjectFile {

    /**
     * 文件ID，自增主键
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 项目ID，不能为空
     */
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    /**
     * 文件名，不能为空，最大长度255
     */
    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    /**
     * 文件路径，不能为空，最大长度500
     */
    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    /**
     * 文件大小，单位字节
     */
    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    /**
     * 文件类型，最大长度50
     */
    @Column(name = "file_type", length = 50)
    private String fileType;

    /**
     * 上传时间
     */
    @Column(name = "upload_time")
    private LocalDateTime uploadTime;

    /**
     * 是否为主文件
     */
    @Column(name = "is_main")
    private Boolean isMain;

    /**
     * 版本号，最大长度20
     */
    @Column(length = 20)
    private String version;

    /**
     * 是否为最新版本
     */
    @Column(name = "is_latest")
    private Boolean isLatest;

    /**
     * 持久化前的处理
     * 设置默认值
     */
    @PrePersist
    public void prePersist() {
        if (uploadTime == null) {
            uploadTime = LocalDateTime.now();
        }
        if (isMain == null) {
            isMain = false;
        }
        if (isLatest == null) {
            isLatest = true;
        }
    }
}