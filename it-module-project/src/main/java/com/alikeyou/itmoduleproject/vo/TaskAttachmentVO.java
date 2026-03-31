package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskAttachmentVO {
    private Long id;
    private Long taskId;
    private String fileName;
    private String filePath;
    private Long fileSizeBytes;
    private String fileType;
    private Long uploadedBy;
    private String uploaderName;
    private LocalDateTime createdAt;
    private Boolean previewSupported;
}
