package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectTemplateVO {
    private Long id;
    private String name;
    private String description;
    private String category;
    private Long creatorId;
    private String creatorName;
    private Boolean isPublic;
    private Integer itemCount;
    private Integer docCount;
    private Integer taskCount;
    private Integer fileCount;
    private Integer folderCount;
    private Integer activityCount;
    private String readmeTitle;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
