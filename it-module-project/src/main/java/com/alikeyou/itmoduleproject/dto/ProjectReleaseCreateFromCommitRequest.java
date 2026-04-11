package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectReleaseCreateFromCommitRequest {
    private Long projectId;
    private Long commitId;
    private String version;
    private String title;
    private String description;
    private String releaseNotes;
    private String releaseType;
    private Boolean recommendedFlag;
}
