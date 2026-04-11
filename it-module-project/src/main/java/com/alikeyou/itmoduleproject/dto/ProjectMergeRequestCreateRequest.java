package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectMergeRequestCreateRequest {
    private Long projectId;
    private Long sourceBranchId;
    private Long targetBranchId;
    private String title;
    private String description;
}
