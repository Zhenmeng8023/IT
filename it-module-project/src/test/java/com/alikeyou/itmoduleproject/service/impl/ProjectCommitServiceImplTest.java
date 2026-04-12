package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBlobRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitChangeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitParentRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCommitServiceImplTest {

    private final ProjectCommitRepository commitRepository = mock(ProjectCommitRepository.class);
    private final ProjectBranchRepository branchRepository = mock(ProjectBranchRepository.class);
    private final ProjectCodeRepositoryRepository repositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectSnapshotItemRepository snapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
    private final ProjectPermissionService permissionService = mock(ProjectPermissionService.class);
    private final ProjectCommitServiceImpl service = new ProjectCommitServiceImpl(
            commitRepository,
            mock(ProjectCommitParentRepository.class),
            mock(ProjectCommitChangeRepository.class),
            branchRepository,
            repositoryRepository,
            mock(ProjectSnapshotRepository.class),
            snapshotItemRepository,
            mock(ProjectFileRepository.class),
            mock(ProjectFileVersionRepository.class),
            mock(ProjectCommitChangeRepository.class),
            mock(ProjectBlobRepository.class),
            permissionService
    );

    @Test
    void rollbackToCommit_shouldRejectWhenTargetSnapshotMatchesCurrentHead() {
        ProjectCommit targetCommit = ProjectCommit.builder()
                .id(11L)
                .repositoryId(101L)
                .branchId(21L)
                .commitNo(1L)
                .displaySha("abc12345")
                .message("target")
                .commitType("normal")
                .snapshotId(501L)
                .createdAt(LocalDateTime.now())
                .build();
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(12L)
                .repositoryId(101L)
                .branchId(21L)
                .commitNo(2L)
                .displaySha("def67890")
                .message("head")
                .commitType("normal")
                .snapshotId(502L)
                .createdAt(LocalDateTime.now())
                .build();
        ProjectBranch branch = ProjectBranch.builder()
                .id(21L)
                .repositoryId(101L)
                .name("main")
                .headCommitId(12L)
                .protectedFlag(false)
                .allowDirectCommitFlag(true)
                .createdAt(LocalDateTime.now())
                .build();
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(101L)
                .projectId(1001L)
                .defaultBranchId(21L)
                .headCommitId(12L)
                .createdAt(LocalDateTime.now())
                .build();
        List<ProjectSnapshotItem> sameSnapshot = List.of(
                ProjectSnapshotItem.builder()
                        .projectFileId(900L)
                        .projectFileVersionId(901L)
                        .blobId(902L)
                        .canonicalPath("src/App.java")
                        .contentHash("hash-902")
                        .build()
        );

        when(commitRepository.findById(11L)).thenReturn(Optional.of(targetCommit));
        when(branchRepository.findById(21L)).thenReturn(Optional.of(branch));
        when(repositoryRepository.findById(101L)).thenReturn(Optional.of(repository));
        when(commitRepository.findById(12L)).thenReturn(Optional.of(headCommit));
        when(snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(501L)).thenReturn(sameSnapshot);
        when(snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(502L)).thenReturn(sameSnapshot);

        BusinessException exception = assertThrows(BusinessException.class, () -> service.rollbackToCommit(11L, 77L));

        assertEquals("目标提交已与当前分支状态一致，无需回退", exception.getMessage());
        verify(permissionService).assertProjectManageMembers(1001L, 77L);
        verify(commitRepository, never()).save(any(ProjectCommit.class));
    }
}
