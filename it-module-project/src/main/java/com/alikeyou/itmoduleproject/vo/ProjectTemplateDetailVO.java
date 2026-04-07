package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectTemplateDetailVO {
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
    private String readmeContent;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<String> savedFileSuffixes;
    private List<String> savedActivityActions;
    private List<ProjectTemplateFileVO> files;
    private List<ProjectTemplateFileVO> docItems;
    private List<ProjectTemplateFileVO> taskItems;
    private List<ProjectTemplateFileVO> fileItems;
    private List<ProjectTemplateFileVO> folderItems;
    private List<ProjectTemplateFileVO> activityItems;
}
