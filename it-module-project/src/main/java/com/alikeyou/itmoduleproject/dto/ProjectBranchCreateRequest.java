package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectBranchCreateRequest {
    private Long projectId;
    private Long sourceBranchId;
    private String name;
    private String branchType;
}
