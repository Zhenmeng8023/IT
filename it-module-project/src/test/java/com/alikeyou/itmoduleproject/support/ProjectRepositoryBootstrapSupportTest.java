package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectRepositoryBootstrapSupportTest {

    private final ProjectBranchRepository projectBranchRepository = mock(ProjectBranchRepository.class);
    private final ProjectCommitRepository projectCommitRepository = mock(ProjectCommitRepository.class);
    private final ProjectSnapshotRepository projectSnapshotRepository = mock(ProjectSnapshotRepository.class);
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
    private final ProjectFileRepository projectFileRepository = mock(ProjectFileRepository.class);
    private final ProjectFileVersionRepository projectFileVersionRepository = mock(ProjectFileVersionRepository.class);
    private final ProjectRepoStorageSupport projectRepoStorageSupport = new ProjectRepoStorageSupport(mock(com.alikeyou.itmoduleproject.repository.ProjectBlobRepository.class)) {
        @Override
        public ProjectBlob saveExistingFile(String storedPath, String originalFilename) {
            return ProjectBlob.builder()
                    .id(700L)
                    .sha256("hash-700")
                    .sizeBytes(128L)
                    .storagePath("upload/project-repo/blobs/hash-700.md")
                    .build();
        }
    };

    private final ProjectRepositoryBootstrapSupport support = new ProjectRepositoryBootstrapSupport(
            projectBranchRepository,
            projectCommitRepository,
            projectSnapshotRepository,
            projectSnapshotItemRepository,
            projectFileRepository,
            projectFileVersionRepository,
            projectRepoStorageSupport
    );

    @Test
    void ensureRepositorySnapshotInitialized_shouldImportLegacyFilesWhenBootstrapStillPristine() {
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(200L)
                .defaultBranchId(300L)
                .createdBy(88L)
                .build();
        ProjectBranch mainBranch = ProjectBranch.builder()
                .id(300L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectBranch devBranch = ProjectBranch.builder()
                .id(301L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectCommit bootstrapCommit = ProjectCommit.builder()
                .id(400L)
                .repositoryId(100L)
                .branchId(300L)
                .snapshotId(500L)
                .message("bootstrap repository")
                .commitType("bootstrap")
                .createdAt(LocalDateTime.now())
                .build();
        ProjectSnapshot snapshot = ProjectSnapshot.builder()
                .id(500L)
                .repositoryId(100L)
                .commitId(400L)
                .fileCount(0)
                .build();
        ProjectFile legacyFile = ProjectFile.builder()
                .id(600L)
                .projectId(200L)
                .fileName("README.md")
                .filePath("upload/project/200/legacy/readme.md")
                .fileSizeBytes(128L)
                .version("1.0.0")
                .build();

        when(projectBranchRepository.findById(300L)).thenReturn(Optional.of(mainBranch));
        when(projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(mainBranch, devBranch));
        when(projectCommitRepository.findById(400L)).thenReturn(Optional.of(bootstrapCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(500L)).thenReturn(List.of());
        when(projectFileRepository.findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(200L)).thenReturn(List.of(legacyFile));
        when(projectFileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(600L)).thenReturn(Optional.empty());
        when(projectFileVersionRepository.countByFileId(600L)).thenReturn(0L);
        when(projectFileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation -> {
            ProjectFileVersion version = invocation.getArgument(0);
            version.setId(1000L);
            return version;
        });
        when(projectFileRepository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(projectSnapshotRepository.findById(500L)).thenReturn(Optional.of(snapshot));
        when(projectCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> invocation.getArgument(0));

        support.ensureRepositorySnapshotInitialized(repository, 99L);

        ArgumentCaptor<List<ProjectSnapshotItem>> snapshotItemsCaptor = ArgumentCaptor.forClass(List.class);
        verify(projectSnapshotItemRepository).saveAll(snapshotItemsCaptor.capture());
        List<ProjectSnapshotItem> addedItems = snapshotItemsCaptor.getValue();
        assertEquals(1, addedItems.size());
        assertEquals("/README.md", addedItems.get(0).getCanonicalPath());
        assertEquals(600L, addedItems.get(0).getProjectFileId());

        ArgumentCaptor<ProjectFile> fileCaptor = ArgumentCaptor.forClass(ProjectFile.class);
        verify(projectFileRepository).save(fileCaptor.capture());
        ProjectFile savedFile = fileCaptor.getValue();
        assertNotNull(savedFile.getLatestCommitId());
        assertEquals(400L, savedFile.getLatestCommitId());
        assertFalse(Boolean.TRUE.equals(savedFile.getDeletedFlag()));

        ArgumentCaptor<ProjectSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ProjectSnapshot.class);
        verify(projectSnapshotRepository).save(snapshotCaptor.capture());
        assertEquals(1, snapshotCaptor.getValue().getFileCount());

        ArgumentCaptor<ProjectCommit> commitCaptor = ArgumentCaptor.forClass(ProjectCommit.class);
        verify(projectCommitRepository).save(commitCaptor.capture());
        assertEquals("初始化仓库并接入现有项目文件", commitCaptor.getValue().getMessage());
    }

    @Test
    void ensureRepositorySnapshotInitialized_shouldSkipWhenSnapshotAlreadyInitialized() {
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(200L)
                .defaultBranchId(300L)
                .build();
        ProjectBranch mainBranch = ProjectBranch.builder()
                .id(300L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectCommit bootstrapCommit = ProjectCommit.builder()
                .id(400L)
                .repositoryId(100L)
                .branchId(300L)
                .snapshotId(500L)
                .message("bootstrap repository")
                .commitType("bootstrap")
                .build();
        ProjectSnapshotItem existingItem = ProjectSnapshotItem.builder()
                .id(900L)
                .snapshotId(500L)
                .projectFileId(901L)
                .projectFileVersionId(902L)
                .blobId(903L)
                .canonicalPath("/src/App.vue")
                .contentHash("hash-903")
                .build();

        when(projectBranchRepository.findById(300L)).thenReturn(Optional.of(mainBranch));
        when(projectCommitRepository.findById(400L)).thenReturn(Optional.of(bootstrapCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(500L)).thenReturn(List.of(existingItem));

        support.ensureRepositorySnapshotInitialized(repository, 99L);

        verify(projectBranchRepository, never()).findByRepositoryIdOrderByCreatedAtAsc(100L);
        verify(projectFileRepository, never()).findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(200L);
        verify(projectSnapshotItemRepository, never()).saveAll(any());
        verify(projectFileRepository, never()).save(any(ProjectFile.class));
    }

    @Test
    void ensureRepositorySnapshotInitialized_shouldSkipWhenAnyBranchHasAdvancedPastBootstrap() {
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(200L)
                .defaultBranchId(300L)
                .build();
        ProjectBranch mainBranch = ProjectBranch.builder()
                .id(300L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectBranch devBranch = ProjectBranch.builder()
                .id(301L)
                .repositoryId(100L)
                .headCommitId(401L)
                .build();
        ProjectCommit bootstrapCommit = ProjectCommit.builder()
                .id(400L)
                .repositoryId(100L)
                .branchId(300L)
                .snapshotId(500L)
                .message("bootstrap repository")
                .commitType("bootstrap")
                .build();

        when(projectBranchRepository.findById(300L)).thenReturn(Optional.of(mainBranch));
        when(projectCommitRepository.findById(400L)).thenReturn(Optional.of(bootstrapCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(500L)).thenReturn(List.of());
        when(projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(mainBranch, devBranch));

        support.ensureRepositorySnapshotInitialized(repository, 99L);

        verify(projectFileRepository, never()).findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(200L);
        verify(projectSnapshotItemRepository, never()).saveAll(any());
        verify(projectFileRepository, never()).save(any(ProjectFile.class));
    }

    @Test
    void ensureRepositorySnapshotInitialized_shouldSkipCommitTrackedProjectFile() {
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(200L)
                .defaultBranchId(300L)
                .createdBy(88L)
                .build();
        ProjectBranch mainBranch = ProjectBranch.builder()
                .id(300L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectBranch devBranch = ProjectBranch.builder()
                .id(301L)
                .repositoryId(100L)
                .headCommitId(400L)
                .build();
        ProjectCommit bootstrapCommit = ProjectCommit.builder()
                .id(400L)
                .repositoryId(100L)
                .branchId(300L)
                .snapshotId(500L)
                .message("bootstrap repository")
                .commitType("bootstrap")
                .build();
        ProjectFile devTrackedFile = ProjectFile.builder()
                .id(600L)
                .projectId(200L)
                .fileName("README.md")
                .filePath("upload/project/200/dev/readme.md")
                .latestCommitId(401L)
                .latestVersionId(1001L)
                .latestBlobId(700L)
                .build();

        when(projectBranchRepository.findById(300L)).thenReturn(Optional.of(mainBranch));
        when(projectBranchRepository.findByRepositoryIdOrderByCreatedAtAsc(100L)).thenReturn(List.of(mainBranch, devBranch));
        when(projectCommitRepository.findById(400L)).thenReturn(Optional.of(bootstrapCommit));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(500L)).thenReturn(List.of());
        when(projectFileRepository.findByProjectIdAndDeletedFlagFalseOrderByUploadTimeDesc(200L)).thenReturn(List.of(devTrackedFile));

        support.ensureRepositorySnapshotInitialized(repository, 99L);

        verify(projectSnapshotItemRepository, never()).saveAll(any());
        verify(projectFileRepository, never()).save(any(ProjectFile.class));
        verify(projectFileVersionRepository, never()).save(any(ProjectFileVersion.class));
    }
}
