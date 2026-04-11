package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ProjectWorkspaceVO {
    private Long id;
    private Long repositoryId;
    private Long branchId;
    private Long ownerId;
    private Long baseCommitId;
    private String status;
    private List<ProjectWorkspaceItemVO> items;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
