package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitParentRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVersionVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ProjectFileServiceImplTest {

    private final ProjectFileRepository projectFileRepository = mock(ProjectFileRepository.class);
    private final ProjectFileVersionRepository projectFileVersionRepository = mock(ProjectFileVersionRepository.class);
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectBranchRepository projectBranchRepository = mock(ProjectBranchRepository.class);
    private final ProjectCommitRepository projectCommitRepository = mock(ProjectCommitRepository.class);
    private final ProjectCommitParentRepository projectCommitParentRepository = mock(ProjectCommitParentRepository.class);
    private final ProjectSnapshotRepository projectSnapshotRepository = mock(ProjectSnapshotRepository.class);
    private final ProjectSnapshotItemRepository projectSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final ProjectWorkspaceService projectWorkspaceService = mock(ProjectWorkspaceService.class);
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport = mock(ProjectRepositoryBootstrapSupport.class);

    private final ProjectFileServiceImpl service = new ProjectFileServiceImpl(
            projectFileRepository,
            projectFileVersionRepository,
            projectCodeRepositoryRepository,
            projectBranchRepository,
            projectCommitRepository,
            projectCommitParentRepository,
            projectSnapshotRepository,
            projectSnapshotItemRepository,
            projectPermissionService,
            fileStorageService,
            projectWorkspaceService,
            projectRepositoryBootstrapSupport
    );

    @Test
    void deleteFile_shouldStageDeleteIntoWorkspaceInsteadOfHardDeleting() {
        ProjectFile projectFile = ProjectFile.builder()
                .id(11L)
                .projectId(1001L)
                .canonicalPath("/src/App.vue")
                .fileName("App.vue")
                .filePath("upload/project/1001/main/app.vue")
                .build();
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(501L)
                .projectId(1001L)
                .defaultBranchId(7L)
                .build();
        ProjectBranch branch = ProjectBranch.builder()
                .id(7L)
                .repositoryId(501L)
                .headCommitId(71L)
                .build();
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(71L)
                .repositoryId(501L)
                .branchId(7L)
                .snapshotId(701L)
                .build();
        ProjectSnapshot snapshot = ProjectSnapshot.builder()
                .id(701L)
                .repositoryId(501L)
                .commitId(71L)
                .fileCount(1)
                .build();
        ProjectSnapshotItem snapshotItem = ProjectSnapshotItem.builder()
                .id(801L)
                .snapshotId(701L)
                .projectFileId(11L)
                .projectFileVersionId(901L)
                .blobId(1001L)
                .canonicalPath("/src/App.vue")
                .contentHash("hash-1001")
                .build();
        ProjectWorkspaceItemVO stagedDelete = ProjectWorkspaceItemVO.builder()
                .id(21L)
                .workspaceId(31L)
                .canonicalPath("/src/App.vue")
                .changeType("DELETE")
                .stagedFlag(true)
                .build();

        when(projectFileRepository.findById(11L)).thenReturn(Optional.of(projectFile));
        when(projectCodeRepositoryRepository.findByProjectId(1001L)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(7L)).thenReturn(Optional.of(branch));
        when(projectCommitRepository.findById(71L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotRepository.findById(701L)).thenReturn(Optional.of(snapshot));
        when(projectSnapshotItemRepository.findBySnapshotIdAndProjectFileId(701L, 11L)).thenReturn(Optional.of(snapshotItem));
        when(projectWorkspaceService.stageDelete(1001L, 7L, 99L, "/src/App.vue")).thenReturn(stagedDelete);

        ProjectWorkspaceItemVO result = service.deleteFile(11L, 7L, 99L);

        assertEquals(21L, result.getId());
        assertEquals("/src/App.vue", result.getCanonicalPath());
        verify(projectPermissionService).assertProjectWritable(1001L, 99L);
        verify(projectWorkspaceService).stageDelete(1001L, 7L, 99L, "/src/App.vue");
        verify(projectFileRepository, never()).delete(projectFile);
        verify(projectFileVersionRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyIterable());
        verify(fileStorageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void listFiles_shouldKeepFeatureBranchViewIsolatedFromDefaultBranch() {
        long projectId = 2001L;
        long repoId = 3001L;
        long defaultBranchId = 10L;
        long featureBranchId = 11L;

        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(repoId)
                .projectId(projectId)
                .defaultBranchId(defaultBranchId)
                .build();
        ProjectBranch defaultBranch = ProjectBranch.builder()
                .id(defaultBranchId)
                .repositoryId(repoId)
                .headCommitId(100L)
                .build();
        ProjectBranch featureBranch = ProjectBranch.builder()
                .id(featureBranchId)
                .repositoryId(repoId)
                .headCommitId(101L)
                .build();
        ProjectCommit defaultHead = ProjectCommit.builder()
                .id(100L)
                .repositoryId(repoId)
                .branchId(defaultBranchId)
                .snapshotId(1000L)
                .build();
        ProjectCommit featureHead = ProjectCommit.builder()
                .id(101L)
                .repositoryId(repoId)
                .branchId(featureBranchId)
                .snapshotId(1001L)
                .build();
        ProjectSnapshotItem defaultSnapshotItem = ProjectSnapshotItem.builder()
                .snapshotId(1000L)
                .projectFileId(501L)
                .projectFileVersionId(601L)
                .blobId(701L)
                .canonicalPath("/src/MainOnly.java")
                .contentHash("hash-main")
                .build();
        ProjectSnapshotItem featureSnapshotItem = ProjectSnapshotItem.builder()
                .snapshotId(1001L)
                .projectFileId(502L)
                .projectFileVersionId(602L)
                .blobId(702L)
                .canonicalPath("/src/FeatureOnly.java")
                .contentHash("hash-feature")
                .build();
        ProjectFileVersion defaultVersion = ProjectFileVersion.builder()
                .id(601L)
                .fileId(501L)
                .version("v1")
                .commitId(100L)
                .serverPath("upload/project/2001/default/MainOnly.java")
                .fileSizeBytes(128L)
                .uploadedAt(LocalDateTime.now().minusHours(1))
                .build();
        ProjectFileVersion featureVersion = ProjectFileVersion.builder()
                .id(602L)
                .fileId(502L)
                .version("v1")
                .commitId(101L)
                .serverPath("upload/project/2001/feature/FeatureOnly.java")
                .fileSizeBytes(256L)
                .uploadedAt(LocalDateTime.now())
                .build();
        Map<Long, ProjectCommit> commitMap = Map.of(
                100L, defaultHead,
                101L, featureHead
        );
        Map<Long, ProjectFileVersion> versionMap = Map.of(
                601L, defaultVersion,
                602L, featureVersion
        );

        when(projectCodeRepositoryRepository.findByProjectId(projectId)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(defaultBranchId)).thenReturn(Optional.of(defaultBranch));
        when(projectBranchRepository.findById(featureBranchId)).thenReturn(Optional.of(featureBranch));
        when(projectCommitRepository.findById(100L)).thenReturn(Optional.of(defaultHead));
        when(projectCommitRepository.findById(101L)).thenReturn(Optional.of(featureHead));
        when(projectSnapshotRepository.findById(1000L)).thenReturn(Optional.of(ProjectSnapshot.builder().id(1000L).build()));
        when(projectSnapshotRepository.findById(1001L)).thenReturn(Optional.of(ProjectSnapshot.builder().id(1001L).build()));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(1000L)).thenReturn(List.of(defaultSnapshotItem));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(1001L)).thenReturn(List.of(featureSnapshotItem));
        when(projectCommitParentRepository.findByCommitIdIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        when(projectCommitRepository.findAllById(anyIterable())).thenAnswer(invocation -> {
            List<ProjectCommit> commits = new ArrayList<>();
            Iterable<Long> ids = invocation.getArgument(0);
            for (Long id : ids) {
                if (commitMap.containsKey(id)) {
                    commits.add(commitMap.get(id));
                }
            }
            return commits;
        });
        when(projectFileVersionRepository.findAllById(anyIterable())).thenAnswer(invocation -> {
            List<ProjectFileVersion> versions = new ArrayList<>();
            Iterable<Long> ids = invocation.getArgument(0);
            for (Long id : ids) {
                if (versionMap.containsKey(id)) {
                    versions.add(versionMap.get(id));
                }
            }
            return versions;
        });
        when(projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(List.of(501L))).thenReturn(List.of(defaultVersion));
        when(projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(List.of(502L))).thenReturn(List.of(featureVersion));
        when(projectFileRepository.findByProjectIdAndIsMainTrue(projectId)).thenReturn(List.of(
                ProjectFile.builder().id(501L).projectId(projectId).isMain(true).build()
        ));

        List<ProjectFileVO> defaultView = service.listFiles(projectId, null, 99L);
        List<ProjectFileVO> featureView = service.listFiles(projectId, featureBranchId, 99L);

        assertEquals(1, defaultView.size());
        assertEquals(501L, defaultView.get(0).getId());
        assertEquals("src/MainOnly.java", defaultView.get(0).getRelativePath());

        assertEquals(1, featureView.size());
        assertEquals(502L, featureView.get(0).getId());
        assertEquals("src/FeatureOnly.java", featureView.get(0).getRelativePath());

        assertFalse(defaultView.stream().anyMatch(file -> file.getId().equals(502L)));
        assertTrue(featureView.stream().noneMatch(file -> file.getId().equals(501L)));
    }

    @Test
    void uploadFile_shouldFailWhenBranchIdMissing() {
        MockMultipartFile multipartFile = new MockMultipartFile(
                "file",
                "readme.md",
                "text/markdown",
                "hello".getBytes()
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.uploadFile(1001L, null, "/README.md", multipartFile, 77L));

        assertEquals("branchId is required for write operations", exception.getMessage());
        verify(projectPermissionService).assertProjectWritable(1001L, 77L);
        verifyNoInteractions(projectCodeRepositoryRepository);
    }

    @Test
    void listPreviewDownloadAndVersions_shouldAllowFeatureHeadInheritedFromSourceBranchCommit() {
        long projectId = 3001L;
        long repoId = 4001L;
        long defaultBranchId = 20L;
        long featureBranchId = 21L;
        long inheritedCommitId = 200L;
        long snapshotId = 300L;
        long fileId = 400L;
        long versionId = 500L;

        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(repoId)
                .projectId(projectId)
                .defaultBranchId(defaultBranchId)
                .build();
        ProjectBranch featureBranch = ProjectBranch.builder()
                .id(featureBranchId)
                .repositoryId(repoId)
                .headCommitId(inheritedCommitId)
                .build();
        ProjectCommit inheritedHead = ProjectCommit.builder()
                .id(inheritedCommitId)
                .repositoryId(repoId)
                .branchId(defaultBranchId)
                .snapshotId(snapshotId)
                .build();
        ProjectSnapshotItem snapshotItem = ProjectSnapshotItem.builder()
                .snapshotId(snapshotId)
                .projectFileId(fileId)
                .projectFileVersionId(versionId)
                .blobId(600L)
                .canonicalPath("/README.md")
                .contentHash("sha-readme")
                .build();
        ProjectFile file = ProjectFile.builder()
                .id(fileId)
                .projectId(projectId)
                .build();
        ProjectFileVersion version = ProjectFileVersion.builder()
                .id(versionId)
                .fileId(fileId)
                .commitId(inheritedCommitId)
                .version("v1")
                .serverPath("upload/project/readme.md")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(projectCodeRepositoryRepository.findByProjectId(projectId)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(featureBranchId)).thenReturn(Optional.of(featureBranch));
        when(projectCommitRepository.findById(inheritedCommitId)).thenReturn(Optional.of(inheritedHead));
        when(projectSnapshotRepository.findById(snapshotId)).thenReturn(Optional.of(ProjectSnapshot.builder().id(snapshotId).build()));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(snapshotId)).thenReturn(List.of(snapshotItem));
        when(projectSnapshotItemRepository.findBySnapshotIdAndProjectFileId(snapshotId, fileId)).thenReturn(Optional.of(snapshotItem));
        when(projectCommitParentRepository.findByCommitIdIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        when(projectCommitRepository.findAllById(anyIterable())).thenReturn(List.of(inheritedHead));
        when(projectFileVersionRepository.findAllById(anyIterable())).thenReturn(List.of(version));
        when(projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(List.of(fileId))).thenReturn(List.of(version));
        when(projectFileVersionRepository.findByFileIdOrderByUploadedAtDesc(fileId)).thenReturn(List.of(version));
        when(projectFileVersionRepository.findById(versionId)).thenReturn(Optional.of(version));
        when(projectFileRepository.findById(fileId)).thenReturn(Optional.of(file));
        when(fileStorageService.loadAsResource("upload/project/readme.md"))
                .thenReturn(new ByteArrayResource("hello".getBytes()));

        List<ProjectFileVO> files = service.listFiles(projectId, featureBranchId, 99L);
        List<ProjectFileVersionVO> versions = service.listVersions(fileId, featureBranchId, 99L);

        assertEquals(1, files.size());
        assertEquals(fileId, files.get(0).getId());
        assertEquals(1, versions.size());
        assertEquals(versionId, versions.get(0).getId());
        assertTrue(service.previewFile(fileId, featureBranchId, 99L).exists());
        assertTrue(service.downloadFile(fileId, featureBranchId, 99L).exists());
    }

    @Test
    void setMainFile_shouldRejectMissingBranchId() {
        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.setMainFile(11L, null, 99L));

        assertEquals("branchId is required for setMainFile", exception.getMessage());
        verifyNoInteractions(projectFileRepository);
    }

    @Test
    void setMainFile_shouldRejectNonDefaultBranch() {
        ProjectFile projectFile = ProjectFile.builder()
                .id(11L)
                .projectId(1001L)
                .build();
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(501L)
                .projectId(1001L)
                .defaultBranchId(7L)
                .build();
        ProjectBranch defaultBranch = ProjectBranch.builder()
                .id(7L)
                .repositoryId(501L)
                .headCommitId(71L)
                .build();
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(71L)
                .repositoryId(501L)
                .branchId(7L)
                .snapshotId(701L)
                .build();

        when(projectFileRepository.findById(11L)).thenReturn(Optional.of(projectFile));
        when(projectCodeRepositoryRepository.findByProjectId(1001L)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(7L)).thenReturn(Optional.of(defaultBranch));
        when(projectCommitRepository.findById(71L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotRepository.findById(701L)).thenReturn(Optional.of(ProjectSnapshot.builder().id(701L).build()));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.setMainFile(11L, 8L, 99L));

        assertEquals("setMainFile is only allowed on default branch view", exception.getMessage());
    }

    @Test
    void setMainFile_shouldSucceedOnDefaultBranchWhenFileExistsInSnapshot() {
        ProjectFile projectFile = ProjectFile.builder()
                .id(11L)
                .projectId(1001L)
                .fileName("README.md")
                .canonicalPath("/README.md")
                .isMain(false)
                .build();
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(501L)
                .projectId(1001L)
                .defaultBranchId(7L)
                .build();
        ProjectBranch defaultBranch = ProjectBranch.builder()
                .id(7L)
                .repositoryId(501L)
                .headCommitId(71L)
                .build();
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(71L)
                .repositoryId(501L)
                .branchId(7L)
                .snapshotId(701L)
                .build();
        ProjectSnapshotItem snapshotItem = ProjectSnapshotItem.builder()
                .snapshotId(701L)
                .projectFileId(11L)
                .projectFileVersionId(901L)
                .canonicalPath("/README.md")
                .build();
        ProjectFileVersion version = ProjectFileVersion.builder()
                .id(901L)
                .fileId(11L)
                .commitId(71L)
                .serverPath("upload/project/readme.md")
                .uploadedAt(LocalDateTime.now())
                .build();

        when(projectFileRepository.findById(11L)).thenReturn(Optional.of(projectFile));
        when(projectCodeRepositoryRepository.findByProjectId(1001L)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(7L)).thenReturn(Optional.of(defaultBranch));
        when(projectCommitRepository.findById(71L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotRepository.findById(701L)).thenReturn(Optional.of(ProjectSnapshot.builder().id(701L).build()));
        when(projectSnapshotItemRepository.findBySnapshotIdAndProjectFileId(701L, 11L)).thenReturn(Optional.of(snapshotItem));
        when(projectSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(701L)).thenReturn(List.of(snapshotItem));
        when(projectFileRepository.findByProjectIdAndIsMainTrue(1001L)).thenReturn(List.of());
        when(projectFileRepository.save(projectFile)).thenReturn(projectFile);
        when(projectCommitParentRepository.findByCommitIdIn(org.mockito.ArgumentMatchers.anyList())).thenReturn(List.of());
        when(projectCommitRepository.findAllById(anyIterable())).thenReturn(List.of(headCommit));
        when(projectFileVersionRepository.findAllById(anyIterable())).thenReturn(List.of(version));
        when(projectFileVersionRepository.findByFileIdInOrderByUploadedAtDesc(List.of(11L))).thenReturn(List.of(version));

        ProjectFileVO result = service.setMainFile(11L, 7L, 99L);

        assertEquals(11L, result.getId());
        assertTrue(Boolean.TRUE.equals(projectFile.getIsMain()));
        verify(projectFileRepository).save(projectFile);
    }

    @Test
    void setMainFile_shouldRejectDefaultBranchWhenFileMissingFromSnapshot() {
        ProjectFile projectFile = ProjectFile.builder()
                .id(11L)
                .projectId(1001L)
                .build();
        ProjectCodeRepository repository = ProjectCodeRepository.builder()
                .id(501L)
                .projectId(1001L)
                .defaultBranchId(7L)
                .build();
        ProjectBranch defaultBranch = ProjectBranch.builder()
                .id(7L)
                .repositoryId(501L)
                .headCommitId(71L)
                .build();
        ProjectCommit headCommit = ProjectCommit.builder()
                .id(71L)
                .repositoryId(501L)
                .branchId(7L)
                .snapshotId(701L)
                .build();

        when(projectFileRepository.findById(11L)).thenReturn(Optional.of(projectFile));
        when(projectCodeRepositoryRepository.findByProjectId(1001L)).thenReturn(Optional.of(repository));
        when(projectBranchRepository.findById(7L)).thenReturn(Optional.of(defaultBranch));
        when(projectCommitRepository.findById(71L)).thenReturn(Optional.of(headCommit));
        when(projectSnapshotRepository.findById(701L)).thenReturn(Optional.of(ProjectSnapshot.builder().id(701L).build()));
        when(projectSnapshotItemRepository.findBySnapshotIdAndProjectFileId(701L, 11L)).thenReturn(Optional.empty());

        BusinessException exception = assertThrows(BusinessException.class,
                () -> service.setMainFile(11L, 7L, 99L));

        assertTrue(exception.getMessage().contains("File is not present"));
        verify(projectFileRepository, never()).save(projectFile);
    }
}
