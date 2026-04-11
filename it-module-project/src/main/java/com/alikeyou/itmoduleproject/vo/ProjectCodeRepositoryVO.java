package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectCodeRepositoryVO {
    private Long id;
    private Long projectId;
    private Long defaultBranchId;
    private Long headCommitId;
    private Long currentReleaseId;
    private Long createdBy;
    private LocalDateTime createdAt;
}
