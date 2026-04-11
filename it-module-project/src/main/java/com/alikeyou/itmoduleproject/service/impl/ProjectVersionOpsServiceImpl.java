package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectReleaseCreateFromCommitRequest;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.entity.ProjectRelease;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMilestoneRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReleaseRepository;
import com.alikeyou.itmoduleproject.service.ProjectVersionOpsService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProjectVersionOpsServiceImpl implements ProjectVersionOpsService {

    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectReleaseRepository projectReleaseRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;

    public ProjectVersionOpsServiceImpl(ProjectMilestoneRepository projectMilestoneRepository,
                                        ProjectReleaseRepository projectReleaseRepository,
                                        ProjectCodeRepositoryRepository projectCodeRepositoryRepository) {
        this.projectMilestoneRepository = projectMilestoneRepository;
        this.projectReleaseRepository = projectReleaseRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
    }

    @Override
    @Transactional
    public ProjectMilestone bindMilestoneCommit(Long milestoneId, Long commitId, Long currentUserId) {
        ProjectMilestone milestone = projectMilestoneRepository.findById(milestoneId)
                .orElseThrow(() -> new BusinessException("里程碑不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(milestone.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        milestone.setRepositoryId(repo.getId());
        milestone.setAnchorCommitId(commitId);
        if (milestone.getFromCommitId() == null) {
            milestone.setFromCommitId(commitId);
        }
        milestone.setToCommitId(commitId);
        if (milestone.getCreatedBy() == null) {
            milestone.setCreatedBy(currentUserId);
        }
        return projectMilestoneRepository.save(milestone);
    }

    @Override
    @Transactional
    public ProjectRelease createReleaseFromCommit(ProjectReleaseCreateFromCommitRequest request, Long currentUserId) {
        if (projectReleaseRepository.existsByProjectIdAndVersion(request.getProjectId(), request.getVersion())) {
            throw new BusinessException("该版本号已经存在");
        }
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        return projectReleaseRepository.save(ProjectRelease.builder()
                .projectId(request.getProjectId())
                .repositoryId(repo.getId())
                .basedCommitId(request.getCommitId())
                .version(request.getVersion())
                .title(request.getTitle())
                .description(request.getDescription())
                .releaseNotes(request.getReleaseNotes())
                .releaseType(request.getReleaseType() == null ? "stable" : request.getReleaseType())
                .status("draft")
                .createdBy(currentUserId)
                .recommendedFlag(Boolean.TRUE.equals(request.getRecommendedFlag()))
                .build());
    }
}
