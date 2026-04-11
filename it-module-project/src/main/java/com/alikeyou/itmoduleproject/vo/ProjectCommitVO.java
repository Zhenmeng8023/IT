package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ProjectCommitVO {
    private Long id;
    private Long repositoryId;
    private Long branchId;
    private Long commitNo;
    private String displaySha;
    private String message;
    private String commitType;
    private Long snapshotId;
    private Long operatorId;
    private Long baseCommitId;
    private Boolean isMergeCommit;
    private Boolean isRevertCommit;
    private Integer changedFileCount;
    private LocalDateTime createdAt;
}
