
package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ProjectMemberStatusVO {
    private Long projectId;
    private boolean member;
    private String role;
    private boolean canApplyJoin;
    private boolean hasPendingJoinRequest;
    private Long pendingJoinRequestId;
    private boolean hasPendingInvite;
    private Long pendingInviteId;
    private String pendingInviteCode;
    private boolean canInviteOthers;
    private boolean canAuditJoinRequests;
}
