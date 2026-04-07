
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectInviteCreateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;

import java.util.List;

public interface ProjectInviteService {

    ProjectInvitationVO createInvite(Long projectId, ProjectInviteCreateRequest request, Long currentUserId);

    List<ProjectInvitationVO> listInvites(Long projectId, Long currentUserId);

    ProjectInvitationVO getInviteDetail(String inviteCode, Long currentUserId);

    ProjectInvitationVO acceptInvite(String inviteCode, Long currentUserId);

    void cancelInvite(Long inviteId, Long currentUserId);
}
