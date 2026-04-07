package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProjectTemplateSaveRequest {
    private String name;
    private String description;
    private String category;
    private Boolean isPublic;

    private Boolean includeFiles;
    private String fileMode;
    private List<Long> selectedFileIds;
    private List<String> fileSuffixes;

    private Boolean includeDocs;
    private Boolean includeReadme;
    private Boolean includeDocVersionHistory;
    private List<Long> selectedDocIds;

    private Boolean includeTasks;
    private Boolean includeTaskDescription;
    private Boolean includeTaskChecklist;
    private Boolean includeTaskAttachments;
    private Boolean includeTaskDependencies;
    private List<Long> selectedTaskIds;

    private Boolean includeActivities;
    private List<String> selectedActivityActions;
    private List<Long> selectedActivityIds;
    private LocalDateTime activityStartTime;
    private LocalDateTime activityEndTime;
}
