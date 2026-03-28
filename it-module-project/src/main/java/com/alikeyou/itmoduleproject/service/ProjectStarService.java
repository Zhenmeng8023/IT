package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectStarStatusVO;

import java.util.Collection;
import java.util.Set;

public interface ProjectStarService {

    ProjectStarStatusVO starProject(Long projectId, Long currentUserId);

    ProjectStarStatusVO unstarProject(Long projectId, Long currentUserId);

    PageResult<ProjectListVO> pageMyStarredProjects(Long currentUserId, int page, int size);

    boolean isStarred(Long projectId, Long currentUserId);

    Set<Long> findStarredProjectIds(Long currentUserId, Collection<Long> projectIds);
}
