package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectWorkspaceItemVO {
    private Long id;
    private Long workspaceId;
    private String canonicalPath;
    private Long blobId;
    private String changeType;
    private Boolean stagedFlag;
    private Boolean conflictFlag;
    private String detectedMessage;
}
