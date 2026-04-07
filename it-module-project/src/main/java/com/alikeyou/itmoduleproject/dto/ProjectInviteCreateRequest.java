
package com.alikeyou.itmoduleproject.dto;

import lombok.Data;

@Data
public class ProjectInviteCreateRequest {
    private Long inviteeId;
    private String inviteeEmail;
    private String inviteRole;
    private String inviteMessage;
    private String expiredAt;
}
