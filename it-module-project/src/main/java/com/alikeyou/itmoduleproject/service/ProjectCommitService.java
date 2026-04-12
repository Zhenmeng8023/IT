package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;

import java.util.List;
import java.util.Map;

public interface ProjectCommitService {
    List<ProjectCommitVO> listByBranch(Long projectId, Long branchId, Long currentUserId);
    Map<String, Object> detail(Long commitId, Long currentUserId);
    Map<String, Object> compare(Long fromCommitId, Long toCommitId, Long currentUserId);
    ProjectCommitVO rollbackToCommit(Long commitId, Long currentUserId);
}
