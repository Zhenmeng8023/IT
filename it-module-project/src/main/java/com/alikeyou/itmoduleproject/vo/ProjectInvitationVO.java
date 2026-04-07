
package com.alikeyou.itmoduleproject.vo;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ProjectInvitationVO {
    private Long id;
    private Long projectId;
    private String projectName;
    private Long inviterId;
    private String inviterName;
    private Long inviteeId;
    private String inviteeName;
    private String inviteeEmail;
    private String inviteRole;
    private String inviteMessage;
    private String inviteCode;
    private String status;
    private LocalDateTime expiredAt;
    private LocalDateTime respondedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
