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
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
public class ProjectBranchServiceImpl implements ProjectBranchService {

    private static final String ERROR_PERMISSION = "[PERMISSION] ";
    private static final String ERROR_BRANCH_STATE = "[BRANCH_STATE] ";

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
        assertProjectReadable(projectId, currentUserId);
        ProjectCodeRepository repo = requireRepositoryByProjectId(projectId);
        projectRepositoryBootstrapSupport.ensureRepositorySnapshotInitialized(repo, currentUserId);
        return projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(repo.getId())
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public ProjectBranchVO create(ProjectBranchCreateRequest request, Long currentUserId) {
        if (request == null || request.getProjectId() == null) {
            throw branchStateException("projectId 不能为空");
        }
        String branchName = normalizeBranchName(request.getName());

        assertProjectManageMembers(request.getProjectId(), currentUserId);
        ProjectCodeRepository repo = requireRepositoryByProjectId(request.getProjectId());

        projectBranchRepository.findByRepositoryIdAndName(repo.getId(), branchName)
                .ifPresent(item -> {
                    throw branchStateException("分支已存在");
                });

        Long sourceBranchId = request.getSourceBranchId() != null
                ? request.getSourceBranchId()
                : repo.getDefaultBranchId();
        if (sourceBranchId == null) {
            throw branchStateException("创建分支必须指定来源分支");
        }

        ProjectBranch source = projectBranchRepository.findById(sourceBranchId)
                .orElseThrow(() -> branchStateException("来源分支不存在"));
        if (!repo.getId().equals(source.getRepositoryId())) {
            throw branchStateException("禁止跨仓库来源分支");
        }
        if (source.getHeadCommitId() == null) {
            throw branchStateException("来源分支缺少有效 head 提交");
        }

        ProjectBranch branch = projectBranchRepository.save(ProjectBranch.builder()
                .repositoryId(repo.getId())
                .name(branchName)
                .branchType(normalizeBranchType(request.getBranchType()))
                .headCommitId(source.getHeadCommitId())
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
                .orElseThrow(() -> branchStateException("分支不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(branch.getRepositoryId())
                .orElseThrow(() -> branchStateException("项目仓库不存在"));
        assertProjectManageMembers(repo.getProjectId(), currentUserId);
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
                .orElseThrow(() -> branchStateException("分支不存在"));
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findById(branch.getRepositoryId())
                .orElseThrow(() -> branchStateException("项目仓库不存在"));
        assertProjectManageMembers(repo.getProjectId(), currentUserId);

        if (Objects.equals(repo.getDefaultBranchId(), branch.getId())) {
            throw branchStateException("默认分支不能删除");
        }
        if (Boolean.TRUE.equals(branch.getProtectedFlag())) {
            throw branchStateException("受保护分支不能删除");
        }
        boolean hasRelatedMergeRequest = projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(repo.getId())
                .stream()
                .anyMatch(mr -> Objects.equals(mr.getSourceBranchId(), branch.getId()) || Objects.equals(mr.getTargetBranchId(), branch.getId()));
        if (hasRelatedMergeRequest) {
            throw branchStateException("分支存在关联的合并请求，无法删除");
        }

        projectBranchRepository.delete(branch);
    }

    private void assertProjectReadable(Long projectId, Long currentUserId) {
        try {
            projectPermissionService.assertProjectReadable(projectId, currentUserId);
        } catch (BusinessException e) {
            throw permissionException(e.getMessage());
        }
    }

    private void assertProjectManageMembers(Long projectId, Long currentUserId) {
        try {
            projectPermissionService.assertProjectManageMembers(projectId, currentUserId);
        } catch (BusinessException e) {
            throw permissionException(e.getMessage());
        }
    }

    private ProjectCodeRepository requireRepositoryByProjectId(Long projectId) {
        return projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> branchStateException("项目仓库不存在，请先初始化仓库"));
    }

    private String normalizeBranchName(String branchName) {
        if (!StringUtils.hasText(branchName)) {
            throw branchStateException("分支名称不能为空");
        }
        String normalized = branchName.trim();
        if (normalized.length() > 100) {
            throw branchStateException("分支名称长度不能超过 100");
        }
        return normalized;
    }

    private String normalizeBranchType(String branchType) {
        return StringUtils.hasText(branchType) ? branchType.trim() : "feature";
    }

    private BusinessException permissionException(String message) {
        return new BusinessException(ERROR_PERMISSION + message);
    }

    private BusinessException branchStateException(String message) {
        return new BusinessException(ERROR_BRANCH_STATE + message);
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
