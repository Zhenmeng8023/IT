package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectWorkspaceCommitRequest {
    private Long projectId;
    private Long branchId;
    private String message;
}
