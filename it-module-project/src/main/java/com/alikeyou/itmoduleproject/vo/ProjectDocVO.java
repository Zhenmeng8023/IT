package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocVO {
    private Long id;
    private Long projectId;
    private String title;
    private String docType;
    private String status;
    private String visibility;
    private Boolean isPrimary;
    private String content;
    private Integer currentVersion;
    private Long creatorId;
    private Long editorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean readmeCandidate;
    private Integer readmePriority;
}
