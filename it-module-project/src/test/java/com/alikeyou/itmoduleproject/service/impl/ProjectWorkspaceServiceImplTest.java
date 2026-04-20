package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspace;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
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
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceRepository;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectWorkspaceServiceImplTest {

    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectBranchRepository projectBranchRepository = mock(ProjectBranchRepository.class);
    private final ProjectWorkspaceRepository projectWorkspaceRepository = mock(ProjectWorkspaceRepository.class);
    private final ProjectWorkspaceItemRepository projectWorkspaceItemRepository = mock(ProjectWorkspaceItemRepository.class);
    private final ProjectBlobRepository projectBlobRepository = mock(ProjectBlobRepository.class);
    private final ProjectRepoStorageSupport projectRepoStorageSupport = mock(ProjectRepoStorageSupport.class);
    private final ProjectCommitRepository projectCommitRepository = mock(ProjectCommitRepository.class);
    private final ProjectCommitParentRepository projectCommitParentRepository = mock(ProjectCommitParentRepository.class);
    private final ProjectSnapshotRepository projectSnapshotRepository = mock(ProjectSnapshotRepository.class);
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
    private final ProjectCommitChangeRepository projectCommitChangeRepository = mock(ProjectCommitChangeRepository.class);
    private final ProjectFileRepository projectFileRepository = mock(ProjectFileRepository.class);
    private final ProjectFileVersionRepository projectFileVersionRepository = mock(ProjectFileVersionRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport = mock(ProjectRepositoryBootstrapSupport.class);

    private final ProjectWorkspaceServiceImpl service = new ProjectWorkspaceServiceImpl(
            projectCodeRepositoryRepository,
            projectBranchRepository,
            projectWorkspaceRepository,
            projectWorkspaceItemRepository,
            projectBlobRepository,
            projectRepoStorageSupport,
            projectCommitRepository,
            projectCommitParentRepository,
            projectSnapshotRepository,
            projectSnapshotItemRepository,
            projectCommitChangeRepository,
            projectFileRepository,
            projectFileVersionRepository,
            projectPermissionService,
            projectRepositoryBootstrapSupport
    );

    @Test
    void commit_shouldSucceedAndRefreshWorkspaceState() {
        ProjectCodeRepository repo = repository(603L, 6003L, 16L);
        ProjectBranch branch = branch(16L, 603L, 1601L, false, true);
        ProjectWorkspace workspace = workspace(9203L, 603L, 16L, 60L, 1601L, "active");

        ProjectWorkspaceItem validAdd = ProjectWorkspaceItem.builder()
                .id(6602L)
                .workspaceId(9203L)
                .canonicalPath("/src/New.vue")
                .blobId(5602L)
                .changeType("ADD")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        ProjectBlob newBlob = ProjectBlob.builder()
                .id(5602L)
                .sha256("sha-5602")
                .storagePath("upload/blob-5602")
                .sizeBytes(20L)
                .build();

        stubWorkspaceContext(repo, branch, workspace, 6003L, 16L, 60L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9203L)).thenReturn(List.of(validAdd));
        when(projectCommitRepository.findById(1601L)).thenReturn(Optional.of(ProjectCommit.builder()
                .id(1601L)
                .repositoryId(603L)
                .branchId(16L)
                .snapshotId(2601L)
                .build()));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(2601L)).thenReturn(List.of());
        when(projectBlobRepository.findById(5602L)).thenReturn(Optional.of(newBlob));

        when(projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(603L, 16L))
                .thenReturn(Optional.of(ProjectCommit.builder().commitNo(3L).build()));
        when(projectCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
            ProjectCommit commit = invocation.getArgument(0);
            if (commit.getId() == null) {
                commit.setId(9601L);
            }
            return commit;
        });

        when(projectFileRepository.findByProjectIdAndCanonicalPathAndDeletedFlagFalse(6003L, "/src/New.vue"))
                .thenReturn(Optional.empty());
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(invocation -> {
            ProjectFile file = invocation.getArgument(0);
            if (file.getId() == null) {
                file.setId(7601L);
            }
            return file;
        });
        when(projectFileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(7601L)).thenReturn(Optional.empty());
        when(projectFileVersionRepository.countByFileId(7601L)).thenReturn(0L);
        when(projectFileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation -> {
            ProjectFileVersion version = invocation.getArgument(0);
            version.setId(8601L);
            return version;
        });

        when(projectSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(9701L);
            return snapshot;
        });
        when(projectSnapshotItemRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectWorkspaceRepository.save(any(ProjectWorkspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.commit(6003L, 16L, 60L, "add one");

        assertEquals(1, result.getChangedFileCount());
        assertEquals(9601L, branch.getHeadCommitId());
        assertEquals("active", workspace.getStatus());
        assertEquals(9601L, workspace.getBaseCommitId());
        verify(projectWorkspaceItemRepository).deleteByWorkspaceId(9203L);
    }

    @Test
    void commit_shouldRejectEmptyWorkspace() {
        ProjectCodeRepository repo = repository(610L, 6010L, 17L);
        ProjectBranch branch = branch(17L, 610L, 1701L, false, true);
        ProjectWorkspace workspace = workspace(9301L, 610L, 17L, 61L, 1701L, "active");

        stubWorkspaceContext(repo, branch, workspace, 6010L, 17L, 61L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9301L)).thenReturn(List.of());

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.commit(6010L, 17L, 61L, "empty"));

        assertTrue(ex.getMessage().contains("[COMMIT_CONTENT]"));
        verify(projectCommitRepository, never()).save(any(ProjectCommit.class));
    }

    @Test
    void commit_shouldRejectProtectedBranchDirectWrite() {
        ProjectCodeRepository repo = repository(620L, 6020L, 18L);
        ProjectBranch branch = branch(18L, 620L, 1801L, true, false);

        when(projectCodeRepositoryRepository.findByProjectId(6020L)).thenReturn(Optional.of(repo));
        when(projectBranchRepository.findById(18L)).thenReturn(Optional.of(branch));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.commit(6020L, 18L, 62L, "blocked"));

        assertTrue(ex.getMessage().contains("[BRANCH_STATE]"));
        verify(projectWorkspaceRepository, never()).upsertActiveWorkspace(any(), any(), any(), any());
    }

    @Test
    void commit_shouldRejectWhenWorkspaceBaselineMissing() {
        ProjectCodeRepository repo = repository(630L, 6030L, 19L);
        ProjectBranch branch = branch(19L, 630L, 1901L, false, true);
        ProjectWorkspace workspace = workspace(9401L, 630L, 19L, 63L, null, "active");

        ProjectWorkspaceItem item = ProjectWorkspaceItem.builder()
                .id(6701L)
                .workspaceId(9401L)
                .canonicalPath("/src/App.vue")
                .blobId(5701L)
                .changeType("MODIFY")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();

        stubWorkspaceContext(repo, branch, workspace, 6030L, 19L, 63L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9401L)).thenReturn(List.of(item));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.commit(6030L, 19L, 63L, "invalid"));

        assertTrue(ex.getMessage().contains("[WORKSPACE_STATE]"));
    }

    @Test
    void stageFile_shouldRejectProtectedBranchDirectWrite() {
        ProjectCodeRepository repo = repository(640L, 6040L, 20L);
        ProjectBranch branch = branch(20L, 640L, 2001L, true, false);
        when(projectCodeRepositoryRepository.findByProjectId(6040L)).thenReturn(Optional.of(repo));
        when(projectBranchRepository.findById(20L)).thenReturn(Optional.of(branch));

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.stageFile(6040L, 20L, 64L, "/src/App.vue",
                        new MockMultipartFile("f", "App.vue", "text/plain", "1".getBytes())));

        assertTrue(ex.getMessage().contains("[BRANCH_STATE]"));
    }

    @Test
    void getCurrentWorkspace_shouldRebaseCleanWorkspaceToBranchHead() {
        ProjectCodeRepository repo = repository(650L, 6050L, 21L);
        ProjectBranch branch = branch(21L, 650L, 2102L, false, true);
        ProjectWorkspace workspace = workspace(9501L, 650L, 21L, 65L, 2101L, "active");

        when(projectCodeRepositoryRepository.findByProjectId(6050L)).thenReturn(Optional.of(repo));
        when(projectBranchRepository.findById(21L)).thenReturn(Optional.of(branch));
        when(projectWorkspaceRepository.upsertActiveWorkspace(650L, 21L, 65L, 2102L)).thenReturn(1);
        when(projectWorkspaceRepository.selectLastInsertId()).thenReturn(9501L);
        when(projectWorkspaceRepository.findById(9501L)).thenReturn(Optional.of(workspace));
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9501L)).thenReturn(List.of());
        when(projectWorkspaceRepository.save(any(ProjectWorkspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.getCurrentWorkspace(6050L, 21L, 65L);

        assertEquals(2102L, result.getBaseCommitId());
        verify(projectWorkspaceRepository).save(workspace);
    }

    @Test
    void commit_shouldRejectWhenPermissionDenied() {
        doThrow(new BusinessException("无权限"))
                .when(projectPermissionService).assertProjectWritable(7000L, 70L);

        BusinessException ex = assertThrows(BusinessException.class,
                () -> service.commit(7000L, 30L, 70L, "nope"));

        assertTrue(ex.getMessage().contains("[PERMISSION]"));
    }

    private void stubWorkspaceContext(ProjectCodeRepository repository,
                                      ProjectBranch branch,
                                      ProjectWorkspace workspace,
                                      Long projectId,
                                      Long branchId,
                                      Long userId) {
        when(projectCodeRepositoryRepository.findByProjectId(projectId)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(projectWorkspaceRepository.upsertActiveWorkspace(
                repository.getId(),
                branch.getId(),
                userId,
                branch.getHeadCommitId()
        )).thenReturn(1);
        when(projectWorkspaceRepository.selectLastInsertId()).thenReturn(workspace.getId());
        when(projectWorkspaceRepository.findById(workspace.getId())).thenReturn(Optional.of(workspace));
        when(projectWorkspaceRepository.findFirstByRepositoryIdAndBranchIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(
                repository.getId(),
                branch.getId(),
                userId,
                "active"
        )).thenReturn(Optional.of(workspace));
    }

    private ProjectCodeRepository repository(Long repositoryId, Long projectId, Long defaultBranchId) {
        return ProjectCodeRepository.builder()
                .id(repositoryId)
                .projectId(projectId)
                .defaultBranchId(defaultBranchId)
                .build();
    }

    private ProjectBranch branch(Long branchId,
                                 Long repositoryId,
                                 Long headCommitId,
                                 boolean protectedFlag,
                                 boolean allowDirectCommitFlag) {
        return ProjectBranch.builder()
                .id(branchId)
                .repositoryId(repositoryId)
                .headCommitId(headCommitId)
                .protectedFlag(protectedFlag)
                .allowDirectCommitFlag(allowDirectCommitFlag)
                .build();
    }

    private ProjectWorkspace workspace(Long workspaceId,
                                       Long repositoryId,
                                       Long branchId,
                                       Long ownerId,
                                       Long baseCommitId,
                                       String status) {
        return ProjectWorkspace.builder()
                .id(workspaceId)
                .repositoryId(repositoryId)
                .branchId(branchId)
                .ownerId(ownerId)
                .baseCommitId(baseCommitId)
                .status(status)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
