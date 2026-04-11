package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectBranchVO {
    private Long id;
    private Long repositoryId;
    private String name;
    private Long headCommitId;
    private String branchType;
    private Boolean protectedFlag;
    private Boolean allowDirectCommitFlag;
    private Long createdBy;
    private LocalDateTime createdAt;
}
