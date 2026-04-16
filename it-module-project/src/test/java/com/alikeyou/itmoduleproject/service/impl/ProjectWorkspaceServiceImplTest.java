package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
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
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void unstagePath_shouldReturnNoOpWhenPathNotStaged() {
        ProjectCodeRepository repository = repository(101L, 1001L, 7L);
        ProjectBranch branch = branch(7L, 101L, 501L);
        ProjectWorkspace workspace = workspace(301L, 101L, 7L, 99L, 501L);
        stubWorkspaceContext(repository, branch, workspace, 1001L, 7L, 99L);

        when(projectWorkspaceItemRepository.findFirstByWorkspaceIdAndCanonicalPath(301L, "/src/App.vue"))
                .thenReturn(Optional.empty());

        ProjectWorkspaceItemVO result = service.unstagePath(1001L, 7L, 99L, "src/App.vue");

        assertEquals(301L, result.getWorkspaceId());
        assertEquals("/src/App.vue", result.getCanonicalPath());
        assertFalse(Boolean.TRUE.equals(result.getStagedFlag()));
        verify(projectWorkspaceItemRepository, never()).deleteById(any(Long.class));
    }

    @Test
    void unstagePath_shouldRemoveExistingStagedItem() {
        ProjectCodeRepository repository = repository(101L, 1001L, 7L);
        ProjectBranch branch = branch(7L, 101L, 501L);
        ProjectWorkspace workspace = workspace(301L, 101L, 7L, 99L, 501L);
        ProjectWorkspaceItem staged = ProjectWorkspaceItem.builder()
                .id(401L)
                .workspaceId(301L)
                .canonicalPath("/src/App.vue")
                .blobId(7001L)
                .changeType("MODIFY")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        stubWorkspaceContext(repository, branch, workspace, 1001L, 7L, 99L);

        when(projectWorkspaceItemRepository.findFirstByWorkspaceIdAndCanonicalPath(301L, "/src/App.vue"))
                .thenReturn(Optional.of(staged));

        ProjectWorkspaceItemVO result = service.unstagePath(1001L, 7L, 99L, "/src/App.vue");

        assertEquals(401L, result.getId());
        assertEquals("/src/App.vue", result.getCanonicalPath());
        assertFalse(Boolean.TRUE.equals(result.getStagedFlag()));
        assertEquals("Unstaged path", result.getDetectedMessage());
        verify(projectWorkspaceItemRepository).deleteById(401L);
    }

    @Test
    void resetWorkspace_shouldClearItemsAndRebaseToBranchHead() {
        ProjectCodeRepository repository = repository(201L, 2001L, 8L);
        ProjectBranch branch = branch(8L, 201L, 900L);
        ProjectWorkspace workspace = workspace(302L, 201L, 8L, 66L, 700L);
        stubWorkspaceContext(repository, branch, workspace, 2001L, 8L, 66L);

        when(projectWorkspaceRepository.save(any(ProjectWorkspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProjectWorkspaceVO result = service.resetWorkspace(2001L, 8L, 66L);

        verify(projectWorkspaceItemRepository).deleteByWorkspaceId(302L);
        verify(projectWorkspaceRepository).save(workspace);
        assertEquals(900L, workspace.getBaseCommitId());
        assertEquals("active", workspace.getStatus());
        assertEquals(900L, result.getBaseCommitId());
        assertEquals(0, result.getItems().size());
    }

    @Test
    void unstagePath_shouldLoadWorkspaceCreatedByAtomicUpsert() {
        ProjectCodeRepository repository = repository(301L, 3001L, 9L);
        ProjectBranch branch = branch(9L, 301L, 1001L);
        ProjectWorkspace existing = workspace(777L, 301L, 9L, 77L, 1001L);

        when(projectCodeRepositoryRepository.findByProjectId(3001L)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(9L)).thenReturn(Optional.of(branch));
        when(projectWorkspaceRepository.upsertActiveWorkspace(301L, 9L, 77L, 1001L)).thenReturn(1);
        when(projectWorkspaceRepository.selectLastInsertId()).thenReturn(777L);
        when(projectWorkspaceRepository.findById(777L)).thenReturn(Optional.of(existing));
        when(projectWorkspaceItemRepository.findFirstByWorkspaceIdAndCanonicalPath(777L, "/src/App.vue"))
                .thenReturn(Optional.empty());

        ProjectWorkspaceItemVO result = service.unstagePath(3001L, 9L, 77L, "/src/App.vue");

        assertEquals(777L, result.getWorkspaceId());
        verify(projectWorkspaceRepository).upsertActiveWorkspace(301L, 9L, 77L, 1001L);
        verify(projectWorkspaceRepository).findById(777L);
        verify(projectWorkspaceRepository, never()).saveAndFlush(any(ProjectWorkspace.class));
    }

    @Test
    void stageFile_shouldUpsertSamePathWithinWorkspace() {
        ProjectCodeRepository repository = repository(401L, 4001L, 12L);
        ProjectBranch branch = branch(12L, 401L, 1201L);
        ProjectWorkspace workspace = workspace(9001L, 401L, 12L, 55L, null);
        stubWorkspaceContext(repository, branch, workspace, 4001L, 12L, 55L);

        ProjectBlob firstBlob = ProjectBlob.builder()
                .id(501L)
                .sha256("sha-1")
                .storagePath("upload/blob-1")
                .build();
        ProjectBlob secondBlob = ProjectBlob.builder()
                .id(502L)
                .sha256("sha-2")
                .storagePath("upload/blob-2")
                .build();
        when(projectRepoStorageSupport.saveMultipart(any(MockMultipartFile.class))).thenReturn(firstBlob, secondBlob);
        when(projectWorkspaceItemRepository.upsertByWorkspaceAndPath(
                eq(9001L),
                eq("/src/App.vue"),
                any(String.class),
                any(Long.class),
                eq("ADD"),
                eq(true),
                eq(false),
                any(String.class)
        )).thenReturn(1);
        when(projectWorkspaceItemRepository.selectLastInsertId()).thenReturn(1234L, 1234L);
        when(projectWorkspaceItemRepository.findById(1234L)).thenReturn(
                Optional.of(ProjectWorkspaceItem.builder()
                        .id(1234L)
                        .workspaceId(9001L)
                        .canonicalPath("/src/App.vue")
                        .blobId(501L)
                        .changeType("ADD")
                        .stagedFlag(true)
                        .conflictFlag(false)
                        .build()),
                Optional.of(ProjectWorkspaceItem.builder()
                        .id(1234L)
                        .workspaceId(9001L)
                        .canonicalPath("/src/App.vue")
                        .blobId(502L)
                        .changeType("ADD")
                        .stagedFlag(true)
                        .conflictFlag(false)
                        .build())
        );

        ProjectWorkspaceItemVO first = service.stageFile(
                4001L,
                12L,
                55L,
                "/src/App.vue",
                new MockMultipartFile("f1", "App.vue", "text/plain", "v1".getBytes())
        );
        ProjectWorkspaceItemVO second = service.stageFile(
                4001L,
                12L,
                55L,
                "/src/App.vue",
                new MockMultipartFile("f2", "App.vue", "text/plain", "v2".getBytes())
        );

        assertEquals(1234L, first.getId());
        assertEquals(1234L, second.getId());
        assertEquals(502L, second.getBlobId());
        verify(projectWorkspaceItemRepository, times(2)).upsertByWorkspaceAndPath(
                eq(9001L),
                eq("/src/App.vue"),
                any(String.class),
                any(Long.class),
                eq("ADD"),
                eq(true),
                eq(false),
                any(String.class)
        );
    }

    @Test
    void stageFiles_shouldRejectDuplicatePathInSameBatch() {
        ProjectCodeRepository repository = repository(501L, 5001L, 13L);
        ProjectBranch branch = branch(13L, 501L, 1301L);
        ProjectWorkspace workspace = workspace(9101L, 501L, 13L, 57L, null);
        stubWorkspaceContext(repository, branch, workspace, 5001L, 13L, 57L);

        ProjectBlob firstBlob = ProjectBlob.builder()
                .id(701L)
                .sha256("sha-701")
                .storagePath("upload/blob-701")
                .build();
        when(projectRepoStorageSupport.saveMultipart(any(MockMultipartFile.class))).thenReturn(firstBlob);
        when(projectWorkspaceItemRepository.upsertByWorkspaceAndPath(
                eq(9101L),
                eq("/src/App.vue"),
                any(String.class),
                eq(701L),
                eq("ADD"),
                eq(true),
                eq(false),
                any(String.class)
        )).thenReturn(1);
        when(projectWorkspaceItemRepository.selectLastInsertId()).thenReturn(90001L);
        when(projectWorkspaceItemRepository.findById(90001L)).thenReturn(Optional.of(ProjectWorkspaceItem.builder()
                .id(90001L)
                .workspaceId(9101L)
                .canonicalPath("/src/App.vue")
                .blobId(701L)
                .changeType("ADD")
                .stagedFlag(true)
                .conflictFlag(false)
                .build()));

        BusinessException exception = assertThrows(BusinessException.class, () -> service.stageFiles(
                5001L,
                13L,
                57L,
                "/",
                java.util.List.of(
                        new MockMultipartFile("f1", "App1.vue", "text/plain", "1".getBytes()),
                        new MockMultipartFile("f2", "App2.vue", "text/plain", "2".getBytes())
                ),
                java.util.List.of("src/App.vue", "src/App.vue")
        ));

        assertEquals(true, exception.getMessage().contains("/src/App.vue"));
        verify(projectWorkspaceItemRepository, times(1)).upsertByWorkspaceAndPath(
                eq(9101L),
                eq("/src/App.vue"),
                any(String.class),
                eq(701L),
                eq("ADD"),
                eq(true),
                eq(false),
                any(String.class)
        );
    }

    @Test
    void commit_shouldClearNoOpModifyAndRejectWhenOnlyNoOpItemsRemain() {
        ProjectCodeRepository repository = repository(601L, 6001L, 14L);
        ProjectBranch branch = branch(14L, 601L, 1401L);
        ProjectWorkspace workspace = workspace(9201L, 601L, 14L, 58L, 1401L);
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(1401L)
                .repositoryId(601L)
                .branchId(14L)
                .snapshotId(2401L)
                .build();
        ProjectSnapshotItem headItem = ProjectSnapshotItem.builder()
                .projectFileId(3401L)
                .projectFileVersionId(4401L)
                .blobId(5401L)
                .canonicalPath("/src/App.vue")
                .contentHash("sha-5401")
                .build();
        ProjectWorkspaceItem noOpModify = ProjectWorkspaceItem.builder()
                .id(6401L)
                .workspaceId(9201L)
                .canonicalPath("/src/App.vue")
                .blobId(5401L)
                .changeType("MODIFY")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        ProjectBlob sameBlob = ProjectBlob.builder()
                .id(5401L)
                .sha256("sha-5401")
                .storagePath("upload/blob-5401")
                .build();

        stubWorkspaceContext(repository, branch, workspace, 6001L, 14L, 58L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9201L)).thenReturn(java.util.List.of(noOpModify));
        when(projectCommitRepository.findById(1401L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(2401L)).thenReturn(java.util.List.of(headItem));
        when(projectBlobRepository.findById(5401L)).thenReturn(Optional.of(sameBlob));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.commit(6001L, 14L, 58L, "sync"));

        assertTrue(exception.getMessage().contains("自动清理"));
        verify(projectWorkspaceItemRepository).deleteAll(java.util.List.of(noOpModify));
        verify(projectCommitRepository, never()).save(any(ProjectCommit.class));
    }

    @Test
    void commit_shouldClearNoOpDeleteAndRejectWhenPathAlreadyMissing() {
        ProjectCodeRepository repository = repository(602L, 6002L, 15L);
        ProjectBranch branch = branch(15L, 602L, 1501L);
        ProjectWorkspace workspace = workspace(9202L, 602L, 15L, 59L, 1501L);
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(1501L)
                .repositoryId(602L)
                .branchId(15L)
                .snapshotId(2501L)
                .build();
        ProjectWorkspaceItem noOpDelete = ProjectWorkspaceItem.builder()
                .id(6402L)
                .workspaceId(9202L)
                .canonicalPath("/src/Missing.vue")
                .changeType("DELETE")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();

        stubWorkspaceContext(repository, branch, workspace, 6002L, 15L, 59L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9202L)).thenReturn(java.util.List.of(noOpDelete));
        when(projectCommitRepository.findById(1501L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(2501L)).thenReturn(java.util.List.of());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.commit(6002L, 15L, 59L, "sync"));

        assertTrue(exception.getMessage().contains("自动清理"));
        verify(projectWorkspaceItemRepository).deleteAll(java.util.List.of(noOpDelete));
        verify(projectCommitRepository, never()).save(any(ProjectCommit.class));
    }

    @Test
    void commit_shouldClearNoOpItemsAndCommitRemainingChanges() {
        ProjectCodeRepository repository = repository(603L, 6003L, 16L);
        ProjectBranch branch = branch(16L, 603L, 1601L);
        ProjectWorkspace workspace = workspace(9203L, 603L, 16L, 60L, 1601L);
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(1601L)
                .repositoryId(603L)
                .branchId(16L)
                .snapshotId(2601L)
                .build();
        ProjectSnapshotItem existingHeadItem = ProjectSnapshotItem.builder()
                .projectFileId(3601L)
                .projectFileVersionId(4601L)
                .blobId(5601L)
                .canonicalPath("/src/App.vue")
                .contentHash("sha-5601")
                .build();
        ProjectWorkspaceItem noOpModify = ProjectWorkspaceItem.builder()
                .id(6601L)
                .workspaceId(9203L)
                .canonicalPath("/src/App.vue")
                .blobId(5601L)
                .changeType("MODIFY")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        ProjectWorkspaceItem validAdd = ProjectWorkspaceItem.builder()
                .id(6602L)
                .workspaceId(9203L)
                .canonicalPath("/src/New.vue")
                .blobId(5602L)
                .changeType("ADD")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        ProjectBlob sameBlob = ProjectBlob.builder()
                .id(5601L)
                .sha256("sha-5601")
                .storagePath("upload/blob-5601")
                .build();
        ProjectBlob newBlob = ProjectBlob.builder()
                .id(5602L)
                .sha256("sha-5602")
                .storagePath("upload/blob-5602")
                .sizeBytes(20L)
                .build();
        ProjectFile savedFile = ProjectFile.builder()
                .id(7601L)
                .projectId(6003L)
                .repositoryId(603L)
                .canonicalPath("/src/New.vue")
                .fileName("New.vue")
                .build();
        ProjectFileVersion savedVersion = ProjectFileVersion.builder()
                .id(8601L)
                .fileId(7601L)
                .blobId(5602L)
                .version("v1")
                .contentHash("sha-5602")
                .build();

        stubWorkspaceContext(repository, branch, workspace, 6003L, 16L, 60L);
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(9203L)).thenReturn(java.util.List.of(noOpModify, validAdd));
        when(projectCommitRepository.findById(1601L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(2601L)).thenReturn(java.util.List.of(existingHeadItem));
        when(projectBlobRepository.findById(5601L)).thenReturn(Optional.of(sameBlob));
        when(projectBlobRepository.findById(5602L)).thenReturn(Optional.of(newBlob));
        when(projectCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(603L, 16L)).thenReturn(Optional.empty());
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
                file.setId(savedFile.getId());
            }
            return file;
        });
        when(projectFileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(7601L)).thenReturn(Optional.empty());
        when(projectFileVersionRepository.countByFileId(7601L)).thenReturn(0L);
        when(projectFileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation -> {
            ProjectFileVersion version = invocation.getArgument(0);
            version.setId(savedVersion.getId());
            return version;
        });
        when(projectSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            snapshot.setId(9701L);
            return snapshot;
        });
        when(projectSnapshotItemRepository.save(any(ProjectSnapshotItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectWorkspaceRepository.save(any(ProjectWorkspace.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var result = service.commit(6003L, 16L, 60L, "add one");

        assertEquals(1, result.getChangedFileCount());
        assertEquals(9601L, branch.getHeadCommitId());
        assertEquals("committed", workspace.getStatus());
        verify(projectWorkspaceItemRepository).deleteAll(java.util.List.of(noOpModify));
        verify(projectCommitChangeRepository).save(any());
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

    private ProjectBranch branch(Long branchId, Long repositoryId, Long headCommitId) {
        return ProjectBranch.builder()
                .id(branchId)
                .repositoryId(repositoryId)
                .headCommitId(headCommitId)
                .build();
    }

    private ProjectWorkspace workspace(Long workspaceId,
                                       Long repositoryId,
                                       Long branchId,
                                       Long ownerId,
                                       Long baseCommitId) {
        return ProjectWorkspace.builder()
                .id(workspaceId)
                .repositoryId(repositoryId)
                .branchId(branchId)
                .ownerId(ownerId)
                .baseCommitId(baseCommitId)
                .status("active")
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
