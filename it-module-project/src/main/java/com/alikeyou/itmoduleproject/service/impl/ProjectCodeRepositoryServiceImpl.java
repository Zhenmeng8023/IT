package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ProjectCodeRepositoryServiceImpl implements ProjectCodeRepositoryService {

    private final ProjectRepository projectRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectSnapshotRepository projectSnapshotRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectCodeRepositoryServiceImpl(ProjectRepository projectRepository,
                                            ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                            ProjectBranchRepository projectBranchRepository,
                                            ProjectCommitRepository projectCommitRepository,
                                            ProjectSnapshotRepository projectSnapshotRepository,
                                            ProjectPermissionService projectPermissionService) {
        this.projectRepository = projectRepository;
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectCommitRepository = projectCommitRepository;
        this.projectSnapshotRepository = projectSnapshotRepository;
        this.projectPermissionService = projectPermissionService;
    }

    @Override
    @Transactional
    public ProjectCodeRepositoryVO initRepository(Long projectId, Long userId) {
        projectPermissionService.assertProjectManageMembers(projectId, userId);
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        ProjectCodeRepository existing = projectCodeRepositoryRepository.findByProjectId(projectId).orElse(null);
        if (existing != null) {
            return toVO(existing);
        }

        ProjectCodeRepository repo = projectCodeRepositoryRepository.save(ProjectCodeRepository.builder()
                .projectId(projectId)
                .createdBy(userId)
                .build());

        ProjectBranch main = projectBranchRepository.save(ProjectBranch.builder()
                .repositoryId(repo.getId())
                .name("main")
                .branchType("main")
                .protectedFlag(true)
                .allowDirectCommitFlag(false)
                .createdBy(userId)
                .build());

        ProjectBranch dev = projectBranchRepository.save(ProjectBranch.builder()
                .repositoryId(repo.getId())
                .name("dev")
                .branchType("dev")
                .protectedFlag(false)
                .allowDirectCommitFlag(true)
                .createdBy(userId)
                .build());

        ProjectSnapshot bootstrapSnapshot = projectSnapshotRepository.save(ProjectSnapshot.builder()
                .repositoryId(repo.getId())
                .manifestHash(UUID.randomUUID().toString().replace("-", ""))
                .fileCount(0)
                .build());

        ProjectCommit bootstrapCommit = projectCommitRepository.save(ProjectCommit.builder()
                .repositoryId(repo.getId())
                .branchId(main.getId())
                .commitNo(1L)
                .displaySha(UUID.randomUUID().toString().replace("-", "").substring(0, 8))
                .message("bootstrap repository")
                .commitType("bootstrap")
                .snapshotId(bootstrapSnapshot.getId())
                .operatorId(userId)
                .isMergeCommit(false)
                .isRevertCommit(false)
                .build());

        bootstrapSnapshot.setCommitId(bootstrapCommit.getId());
        projectSnapshotRepository.save(bootstrapSnapshot);

        main.setHeadCommitId(bootstrapCommit.getId());
        dev.setHeadCommitId(bootstrapCommit.getId());
        projectBranchRepository.save(main);
        projectBranchRepository.save(dev);

        repo.setDefaultBranchId(main.getId());
        repo.setHeadCommitId(bootstrapCommit.getId());
        projectCodeRepositoryRepository.save(repo);

        project.setUpdatedAt(project.getUpdatedAt());
        return toVO(repo);
    }

    @Override
    public ProjectCodeRepositoryVO getByProjectId(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectCodeRepositoryRepository.findByProjectId(projectId)
                .map(this::toVO)
                .orElse(null);
    }

    private ProjectCodeRepositoryVO toVO(ProjectCodeRepository repo) {
        return ProjectCodeRepositoryVO.builder()
                .id(repo.getId())
                .projectId(repo.getProjectId())
                .defaultBranchId(repo.getDefaultBranchId())
                .headCommitId(repo.getHeadCommitId())
                .currentReleaseId(repo.getCurrentReleaseId())
                .createdBy(repo.getCreatedBy())
                .createdAt(repo.getCreatedAt())
                .build();
    }
}
