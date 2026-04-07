
package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestAuditRequest;
import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestCreateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectJoinRequestVO;

import java.util.List;

public interface ProjectJoinRequestService {

    ProjectJoinRequestVO submitJoinRequest(Long projectId, ProjectJoinRequestCreateRequest request, Long currentUserId);

    List<ProjectJoinRequestVO> listJoinRequests(Long projectId, Long currentUserId);

    ProjectJoinRequestVO getMyJoinRequestStatus(Long projectId, Long currentUserId);

    ProjectJoinRequestVO auditJoinRequest(Long requestId, ProjectJoinRequestAuditRequest request, Long currentUserId);

    void cancelJoinRequest(Long requestId, Long currentUserId);
}
