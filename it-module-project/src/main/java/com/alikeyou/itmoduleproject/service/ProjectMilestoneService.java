package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.entity.ProjectMilestone;

import java.util.List;
import java.util.Map;

public interface ProjectMilestoneService {

    List<ProjectMilestone> listMilestones(Long projectId, String status, Long currentUserId);

    ProjectMilestone createMilestone(ProjectMilestone request, Long currentUserId);

    ProjectMilestone updateMilestone(Long id, ProjectMilestone request, Long currentUserId);

    ProjectMilestone changeStatus(Long id, String status, Long currentUserId);

    void deleteMilestone(Long id, Long currentUserId);

    Map<String, Object> getOverview(Long projectId, Long currentUserId);
}
