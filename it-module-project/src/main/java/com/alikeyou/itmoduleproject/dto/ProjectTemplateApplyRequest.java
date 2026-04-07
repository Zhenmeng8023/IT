package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

import java.util.List;

@Data
public class ProjectTemplateApplyRequest {
    private String projectName;
    private String projectDescription;
    private String category;
    private String visibility;
    private String tags;

    private Boolean applyFiles;
    private String fileMode;
    private List<String> fileSuffixes;
    private List<Long> selectedTemplateFileIds;
    private Boolean createFolders;

    private Boolean applyDocs;
    private Boolean applyReadme;
    private Boolean applyDocVersionHistory;
    private List<Long> selectedTemplateDocIds;

    private Boolean applyTasks;
    private Boolean applyTaskDescription;
    private Boolean applyTaskChecklist;
    private Boolean applyTaskAttachments;
    private Boolean applyTaskDependencies;
    private List<Long> selectedTemplateTaskIds;

    private Boolean applyActivities;
    private String activityMode;
    private List<Long> selectedTemplateActivityIds;
}
