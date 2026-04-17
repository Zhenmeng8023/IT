package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.service.ProjectBranchService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectBranchServiceImplTest {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectBranchRepository projectBranchRepository = mock(ProjectBranchRepository.class);
    private final ProjectMergeRequestRepository projectMergeRequestRepository = mock(ProjectMergeRequestRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport = mock(ProjectRepositoryBootstrapSupport.class);
    private final ProjectBranchService service = new ProjectBranchServiceImpl(
            projectCodeRepositoryRepository,
            projectBranchRepository,
            projectMergeRequestRepository,
            projectPermissionService,
            projectRepositoryBootstrapSupport
    );

    @Test
    void deleteBranch_shouldRejectDefaultBranch() {
        ProjectCodeRepository repo = repo(100L, 9L, 11L);
        ProjectBranch branch = branch(11L, 100L, false);
        when(projectBranchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(projectCodeRepositoryRepository.findById(100L)).thenReturn(Optional.of(repo));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteBranch(11L, 99L));

        assertTrue(ex.getMessage().contains("默认分支"));
        verify(projectBranchRepository, never()).delete(branch);
    }

    @Test
    void deleteBranch_shouldRejectProtectedBranch() {
        ProjectCodeRepository repo = repo(100L, 9L, 12L);
        ProjectBranch branch = branch(11L, 100L, true);
        when(projectBranchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(projectCodeRepositoryRepository.findById(100L)).thenReturn(Optional.of(repo));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteBranch(11L, 99L));

        assertTrue(ex.getMessage().contains("受保护分支"));
        verify(projectBranchRepository, never()).delete(branch);
    }

    @Test
    void deleteBranch_shouldRejectBranchWithRelatedMergeRequest() {
        ProjectCodeRepository repo = repo(100L, 9L, 12L);
        ProjectBranch branch = branch(11L, 100L, false);
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(1L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .build();
        when(projectBranchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(projectCodeRepositoryRepository.findById(100L)).thenReturn(Optional.of(repo));
        when(projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(100L)).thenReturn(List.of(mr));

        BusinessException ex = assertThrows(BusinessException.class, () -> service.deleteBranch(11L, 99L));

        assertTrue(ex.getMessage().contains("关联的合并请求"));
        verify(projectBranchRepository, never()).delete(branch);
    }

    @Test
    void deleteBranch_shouldDeleteNormalBranch() {
        ProjectCodeRepository repo = repo(100L, 9L, 12L);
        ProjectBranch branch = branch(11L, 100L, false);
        when(projectBranchRepository.findById(11L)).thenReturn(Optional.of(branch));
        when(projectCodeRepositoryRepository.findById(100L)).thenReturn(Optional.of(repo));
        when(projectMergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(100L)).thenReturn(List.of());

        service.deleteBranch(11L, 99L);

        verify(projectBranchRepository).delete(branch);
    }

    private ProjectCodeRepository repo(Long repositoryId, Long projectId, Long defaultBranchId) {
        return ProjectCodeRepository.builder()
                .id(repositoryId)
                .projectId(projectId)
                .defaultBranchId(defaultBranchId)
                .build();
    }

    private ProjectBranch branch(Long branchId, Long repositoryId, boolean protectedFlag) {
        return ProjectBranch.builder()
                .id(branchId)
                .repositoryId(repositoryId)
                .protectedFlag(protectedFlag)
                .build();
    }
}
