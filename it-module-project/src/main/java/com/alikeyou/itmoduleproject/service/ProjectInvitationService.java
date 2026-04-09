package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectInvitationCreateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;

import java.util.List;

public interface ProjectInvitationService {
    ProjectInvitationVO createInvitation(ProjectInvitationCreateRequest request, Long currentUserId);

    List<ProjectInvitationVO> listProjectInvitations(Long projectId, String status, Long currentUserId);

    List<ProjectInvitationVO> listMyPendingInvitations(Long currentUserId);

    ProjectInvitationVO acceptInvitation(Long invitationId, Long currentUserId);

    ProjectInvitationVO rejectInvitation(Long invitationId, Long currentUserId);

    void cancelInvitation(Long invitationId, Long currentUserId);
}
