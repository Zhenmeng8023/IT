package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectBranchCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.service.ProjectBranchService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectBranchVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ProjectBranchServiceImpl implements ProjectBranchService {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectBranchRepository projectBranchRepository;

    public ProjectBranchServiceImpl(ProjectCodeRepositoryRepository projectCodeRepositoryRepository,
                                    ProjectBranchRepository projectBranchRepository) {
        this.projectCodeRepositoryRepository = projectCodeRepositoryRepository;
        this.projectBranchRepository = projectBranchRepository;
    }

    @Override
    public List<ProjectBranchVO> listByProjectId(Long projectId) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(projectId)
                .orElseThrow(() -> new BusinessException("项目仓库不存在，请先初始化仓库"));
        return projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(repo.getId())
                .stream().map(this::toVO).toList();
    }

    @Override
    @Transactional
    public ProjectBranchVO create(ProjectBranchCreateRequest request, Long currentUserId) {
        ProjectCodeRepository repo = projectCodeRepositoryRepository.findByProjectId(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目仓库不存在，请先初始化仓库"));
        projectBranchRepository.findByRepositoryIdAndName(repo.getId(), request.getName())
                .ifPresent(item -> { throw new BusinessException("分支已存在"); });
        ProjectBranch source = null;
        if (request.getSourceBranchId() != null) {
            source = projectBranchRepository.findById(request.getSourceBranchId())
                    .orElseThrow(() -> new BusinessException("源分支不存在"));
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
    public ProjectBranchVO updateProtection(Long branchId, Boolean protectedFlag, Boolean allowDirectCommitFlag) {
        ProjectBranch branch = projectBranchRepository.findById(branchId)
                .orElseThrow(() -> new BusinessException("分支不存在"));
        if (protectedFlag != null) {
            branch.setProtectedFlag(protectedFlag);
        }
        if (allowDirectCommitFlag != null) {
            branch.setAllowDirectCommitFlag(allowDirectCommitFlag);
        }
        return toVO(projectBranchRepository.save(branch));
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
