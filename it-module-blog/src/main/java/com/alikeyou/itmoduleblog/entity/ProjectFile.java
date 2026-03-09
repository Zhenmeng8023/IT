package com.alikeyou.itmoduleblog.entity;
//ProjectFile 实体类用于管理项目相关的文件，映射到数据库的 project_file 表，主要用于存储项目的附件和相关文件。

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "project_file", schema = "it_data")
public class ProjectFile {
    @Id
    @Column(name = "id", nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_path", nullable = false, length = 500)
    private String filePath;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "file_type", length = 50)
    private String fileType;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "upload_time")
    private Instant uploadTime;

    @ColumnDefault("0")
    @Column(name = "is_main")
    private Boolean isMain;

}