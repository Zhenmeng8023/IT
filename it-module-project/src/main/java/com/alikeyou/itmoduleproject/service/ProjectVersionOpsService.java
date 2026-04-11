package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.dto.ProjectReleaseCreateFromCommitRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.entity.ProjectRelease;

public interface ProjectVersionOpsService {
    ProjectMilestone bindMilestoneCommit(Long milestoneId, Long commitId, Long currentUserId);
    ProjectRelease createReleaseFromCommit(ProjectReleaseCreateFromCommitRequest request, Long currentUserId);
}
