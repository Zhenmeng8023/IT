package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectFileVersionVO {
    private Long id;
    private Long fileId;
    private String version;
    private String serverPath;
    private Long fileSizeBytes;
    private Long uploadedBy;
    private String commitMessage;
    private LocalDateTime uploadedAt;
}
