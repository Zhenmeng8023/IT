package com.alikeyou;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
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
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.service.impl.ProjectWorkspaceServiceImpl;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectWorkspaceCurrentIntegrationTest {

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

    private final ProjectWorkspaceService projectWorkspaceService = new ProjectWorkspaceServiceImpl(
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
    void currentWorkspaceShouldResolveExistingActiveWorkspace() {
        Long projectId = 7001L;
        Long repositoryId = 8001L;
        Long branchId = 9001L;
        Long ownerId = 77L;
        Long headCommitId = 10001L;
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(repositoryId)
                .projectId(projectId)
                .defaultBranchId(branchId)
                .build();
        ProjectBranch branch = ProjectBranch.builder()
                .id(branchId)
                .repositoryId(repositoryId)
                .headCommitId(headCommitId)
                .build();
        ProjectWorkspace existing = ProjectWorkspace.builder()
                .id(11001L)
                .repositoryId(repositoryId)
                .branchId(branchId)
                .ownerId(ownerId)
                .baseCommitId(headCommitId)
                .status("active")
                .createdAt(LocalDateTime.now().minusMinutes(5))
                .updatedAt(LocalDateTime.now())
                .build();
        ProjectWorkspaceItem item = ProjectWorkspaceItem.builder()
                .id(12001L)
                .workspaceId(existing.getId())
                .canonicalPath("/src/dev-only.txt")
                .blobId(13001L)
                .changeType("ADD")
                .stagedFlag(true)
                .conflictFlag(false)
                .build();
        ProjectBlob blob = ProjectBlob.builder()
                .id(13001L)
                .sha256("sha-dev-only")
                .storagePath("upload/project-repo/blobs/sha-dev-only.txt")
                .sizeBytes(32L)
                .mimeType("text/plain")
                .build();

        when(projectCodeRepositoryRepository.findByProjectId(projectId)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(branchId)).thenReturn(Optional.of(branch));
        when(projectWorkspaceRepository.upsertActiveWorkspace(repositoryId, branchId, ownerId, headCommitId)).thenReturn(1);
        when(projectWorkspaceRepository.selectLastInsertId()).thenReturn(existing.getId());
        when(projectWorkspaceRepository.findById(existing.getId())).thenReturn(Optional.of(existing));
        when(projectWorkspaceItemRepository.findByWorkspaceIdOrderByIdAsc(existing.getId())).thenReturn(List.of(item));
        when(projectBlobRepository.findById(blob.getId())).thenReturn(Optional.of(blob));
        when(projectWorkspaceItemRepository.save(item)).thenReturn(item);

        ProjectWorkspaceVO workspace = projectWorkspaceService.getCurrentWorkspace(projectId, branchId, ownerId);

        assertNotNull(workspace);
        assertEquals(branchId, workspace.getBranchId());
        assertEquals(ownerId, workspace.getOwnerId());
        assertEquals("active", workspace.getStatus());
        assertEquals(headCommitId, workspace.getBaseCommitId());
        assertEquals(1, workspace.getItems().size());
        assertEquals("/src/dev-only.txt", workspace.getItems().get(0).getCanonicalPath());
        assertFalse(Boolean.TRUE.equals(workspace.getItems().get(0).getConflictFlag()));
        assertEquals(1, workspace.getChanges().size());
        assertEquals("/src/dev-only.txt", workspace.getChanges().get(0).getNewPath());

        verify(projectPermissionService).assertProjectReadable(projectId, ownerId);
        verify(projectWorkspaceRepository).upsertActiveWorkspace(repositoryId, branchId, ownerId, headCommitId);
        verify(projectWorkspaceItemRepository).save(item);
    }
}
