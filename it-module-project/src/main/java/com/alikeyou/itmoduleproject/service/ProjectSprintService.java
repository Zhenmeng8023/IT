package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectSprint;

import java.util.List;
import java.util.Map;

public interface ProjectSprintService {

    List<ProjectSprint> listSprints(Long projectId, String status, Long currentUserId);

    ProjectSprint createSprint(ProjectSprint request, Long currentUserId);

    ProjectSprint updateSprint(Long id, ProjectSprint request, Long currentUserId);

    ProjectSprint changeStatus(Long id, String status, Long currentUserId);

    Map<String, Object> getCurrentSprintSummary(Long projectId, Long currentUserId);
}
