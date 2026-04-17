package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectBranchCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.service.ProjectBranchService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.vo.ProjectBranchVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ProjectBranchServiceImpl implements ProjectBranchService {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;
    private final ProjectMergeRequestRepository projectMergeRequestRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport;

    public ProjectBranchServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                    ProjectBranchRepository projectBranchRepository,
                                    ProjectMergeRequestRepository projectMergeRequestRepository,
                                    ProjectPermissionService projectPermissionService,
                                    ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
        this.projectMergeRequestRepository = projectMergeRequestRepository;
        this.projectPermissionService = projectPermissionService;
        this.projectRepositoryBootstrapSupport = projectRepositoryBootstrapSupport;
    }

    @Override
    public List<ProjectBranchVO> listByProjectId(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在，请先初始化仓库"));
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repo, currentUserId);
        return projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(repo.getId())
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public ProjectBranchVO create(ProjectBranchCreateRequest request, Long currentUserId) {
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在，请先初始化仓库"));
        projectBranchRepository.findByRepositoryIdAndName(repo.getId(), request.getName())
                .ifPresent(item -> { throw new BusinessException("分支已存在"); });
        ProjectBranch source = null;
        if (request.getSourceBranchId() != null) {
            source = projectBranchRepository.findById(request.getSourceBranchId())
                    .orElseThrow(() -> new BusinessException("源分支不存在"));
            if (!repo.getId().equals(source.getRepositoryId())) {
                throw new BusinessException("禁止使用其他仓库的 source branch 创建当前仓库分支");
            }
        }
        ProjectBranch branch = projectBranchRepository.save(ProjectBranch.builder()
                .repositoryId(repo.getId())
                .name(request.getName())
                .branchType(request.getBranchType() == null ? "feature" : request.getBranchType())
                .headCommitId(source == null ? null : source.getHeadCommitId())
                .protectedFlag(false)
                .allowDirectCommitFlag(true)
                .createdBy(currentUserId)
                .build());
        return toVO(branch);
    }

    @Override
    @Transactional
    public ProjectBranchVO updateProtection(Long branchId, Boolean protectedFlag, Boolean allowDirectCommitFlag, Long currentUserId) {
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(branch.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);
        if (protectedFlag != null) {
            branch.setProtectedFlag(protectedFlag);
        }
        if (allowDirectCommitFlag != null) {
            branch.setAllowDirectCommitFlag(allowDirectCommitFlag);
        }
        return toVO(projectBranchRepository.save(branch));
    }

    @Override
    @Transactional
    public void deleteBranch(Long branchId, Long currentUserId) {
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(branch.getRepositoryId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在"));
        projectPermissionService.assertProjectManageMembers(repo.getProjectId(), currentUserId);

        if (Objects.equals(repo.getDefaultBranchId(), branch.getId())) {
            throw new BusinessException("默认分支不能删除");
        }
        if (Boolean.TRUE.equals(branch.getProtectedFlag())) {
            throw new BusinessException("受保护分支不能删除");
        }
        boolean hasRelatedMergeRequest = projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repo.getId())
                .stream()
                .anyMatch(mr -> Objects.equals(mr.getSourceBranchId(), branch.getId()) || Objects.equals(mr.getTargetBranchId(), branch.getId()));
        if (hasRelatedMergeRequest) {
            throw new BusinessException("该分支存在关联的合并请求，无法删除");
        }

        projectBranchRepository.delete(branch);
    }

    private ProjectBranchVO toVO(ProjectBranch branch) {
        return ProjectBranchVO.builder()
                .id(branch.getId())
                .repositoryId(branch.getRepositoryId())
                .name(branch.getName())
                .headCommitId(branch.getHeadCommitId())
                .branchType(branch.getBranchType())
                .protectedFlag(branch.getProtectedFlag())
                .allowDirectCommitFlag(branch.getAllowDirectCommitFlag())
                .createdBy(branch.getCreatedBy())
                .createdAt(branch.getCreatedAt())
                .build();
    }
}
