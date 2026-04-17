package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectMergeRequestVO {
    private Long id;
    private Long repositoryId;
    private Long sourceBranchId;
    private Long targetBranchId;
    private Long sourceHeadCommitId;
    private Long targetHeadCommitId;
    private String title;
    private String description;
    private String status;
    private Long createdBy;
    private Long mergedBy;
    private LocalDateTime mergedAt;
    private LocalDateTime createdAt;
    private List<Object> reviews;
    private List<ProjectCheckRunVO> checks;
    private List<ProjectCheckRunVO> effectiveChecks;
}
