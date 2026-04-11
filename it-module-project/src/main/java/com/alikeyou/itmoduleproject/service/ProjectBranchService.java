package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectBranchCreateRequest;
import com.alikeyou.itmoduleproject.vo.ProjectBranchVO;

import java.util.List;

public interface ProjectBranchService {
    List<ProjectBranchVO> listByProjectId(Long projectId);
    ProjectBranchVO create(ProjectBranchCreateRequest request, Long currentUserId);
    ProjectBranchVO updateProtection(Long branchId, Boolean protectedFlag, Boolean allowDirectCommitFlag);
}
