package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectDocListItemVO {
    private Long id;
    private Long projectId;
    private String title;
    private String docType;
    private String status;
    private String visibility;
    private Integer currentVersion;
    private Long creatorId;
    private Long editorId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String excerpt;
    private Boolean readmeCandidate;
}
