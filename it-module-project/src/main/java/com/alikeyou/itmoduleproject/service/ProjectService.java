package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectUpdateRequest;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;

import java.util.List;

public interface ProjectService {

    ProjectDetailVO createProject(ProjectCreateRequest request, Long userId);

    ProjectDetailVO updateProject(Long id, ProjectUpdateRequest request, Long userId);

    ProjectDetailVO getProjectDetail(Long id, Long userId);

    List<ProjectMemberVO> listProjectContributors(Long projectId, Long currentUserId);

    List<ProjectListVO> listRelatedProjects(Long projectId, Long currentUserId, int size);

    PageResult<ProjectListVO> pageProjects(String keyword,
                                           String status,
                                           Long authorId,
                                           String visibility,
                                           String category,
                                           String tag,
                                           String sortBy,
                                           Long currentUserId,
                                           int page,
                                           int size);

    PageResult<ProjectListVO> pageMyProjects(Long userId, int page, int size);

    PageResult<ProjectListVO> pageParticipatedProjects(Long userId, int page, int size);

    void deleteProject(Long id, Long userId);
}
