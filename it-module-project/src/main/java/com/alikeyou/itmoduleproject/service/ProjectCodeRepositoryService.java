package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;

public interface ProjectCodeRepositoryService {
    ProjectCodeRepositoryVO initRepository(Long projectId, Long userId);
    ProjectCodeRepositoryVO getByProjectId(Long projectId);
}
