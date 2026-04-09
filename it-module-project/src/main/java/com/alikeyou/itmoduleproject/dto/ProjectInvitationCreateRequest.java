package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectInvitationCreateRequest {
    private Long projectId;
    private Long inviteeId;
    private String inviteRole;
    private String inviteMessage;
    private Integer expireDays;
}
