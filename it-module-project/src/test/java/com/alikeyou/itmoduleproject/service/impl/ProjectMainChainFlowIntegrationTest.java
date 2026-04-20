package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.dto.ConflictResolutionRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitChange;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.entity.ProjectFileVersion;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
import com.alikeyou.itmoduleproject.entity.ProjectReview;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspace;
import com.alikeyou.itmoduleproject.entity.ProjectWorkspaceItem;
import com.alikeyou.itmoduleproject.repository.ProjectActivityLogRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBlobRepository;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCheckRunRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitChangeRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitParentRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMergeRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectReviewRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceItemRepository;
import com.alikeyou.itmoduleproject.repository.ProjectWorkspaceRepository;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.ConflictType;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.vo.ProjectCommitVO;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectMainChainFlowIntegrationTest {

    @Test
    void mainChain_shouldCoverUploadWorkspaceCommitMrConflictAndResolveThenMerge() {
        Harness harness = new Harness();
        harness.seedInitialGraph();

        MockMultipartFile readme = new MockMultipartFile(
                "file",
                "README.md",
                "text/plain",
                "# project chain".getBytes()
        );
        harness.fileService.uploadFile(
                harness.projectId,
                harness.featureBranchId,
                "/docs/README.md",
                readme,
                harness.userId
        );

        ProjectWorkspaceVO workspace = harness.workspaceService.getCurrentWorkspace(
                harness.projectId,
                harness.featureBranchId,
                harness.userId
        );
        assertEquals("active", workspace.getStatus());
        assertEquals(1, workspace.getItems().size());
        assertEquals("/docs/README.md", workspace.getItems().get(0).getCanonicalPath());
        assertTrue(Boolean.TRUE.equals(workspace.getItems().get(0).getStagedFlag()));

        ProjectCommitVO featureCommit = harness.workspaceService.commit(
                harness.projectId,
                harness.featureBranchId,
                harness.userId,
                "feat: add README for integration chain"
        );
        assertEquals(1, featureCommit.getChangedFileCount());
        assertEquals(featureCommit.getId(), harness.branch(harness.featureBranchId).getHeadCommitId());

        ProjectMergeRequestCreateRequest createRequest = new ProjectMergeRequestCreateRequest();
        createRequest.setProjectId(harness.projectId);
        createRequest.setSourceBranchId(harness.featureBranchId);
        createRequest.setTargetBranchId(harness.mainBranchId);
        createRequest.setTitle("chain-main");
        createRequest.setDescription("thread-4 flow");
        ProjectMergeRequestVO mr = harness.mergeRequestService.create(createRequest, harness.userId);
        assertEquals("open", mr.getStatus());

        MergeCheckResult staleCheck = harness.mergeRequestService.checkMerge(mr.getId(), harness.userId);
        assertFalse(Boolean.TRUE.equals(staleCheck.getMergeable()));
        String staleConflictId = staleCheck.getConflicts().stream()
                .filter(conflict -> ConflictType.STALE_BRANCH.equals(conflict.getConflictType()))
                .map(conflict -> conflict.getConflictId())
                .findFirst()
                .orElseThrow();

        assertThrows(BusinessException.class, () -> harness.mergeRequestService.merge(mr.getId(), harness.userId));

        ConflictResolutionRequest resolveRequest = ConflictResolutionRequest.builder()
                .options(List.of(ConflictResolutionOption.builder()
                        .conflictId(staleConflictId)
                        .resolutionStrategy("SYNC_SOURCE_WITH_TARGET")
                        .build()))
                .build();
        var resolution = harness.mergeRequestService.resolveStructuredConflicts(mr.getId(), resolveRequest, harness.userId);
        assertNotNull(resolution.getSupplementalCommitId());
        assertTrue(Boolean.TRUE.equals(resolution.getRequiresRecheck()));

        MergeCheckResult postResolveCheck = harness.mergeRequestService.checkMerge(mr.getId(), harness.userId);
        assertTrue(Boolean.TRUE.equals(postResolveCheck.getMergeable()));
        assertTrue(postResolveCheck.getConflicts() == null || postResolveCheck.getConflicts().isEmpty());

        ProjectMergeRequestVO merged = harness.mergeRequestService.merge(mr.getId(), harness.userId);
        assertEquals("merged", merged.getStatus());
        assertNotNull(merged.getMergedAt());
        assertTrue(Objects.equals(harness.repository().getHeadCommitId(), harness.branch(harness.mainBranchId).getHeadCommitId()));
    }

    private static class Harness {
        private final long projectId = 9001L;
        private final long repositoryId = 9101L;
        private final long mainBranchId = 9201L;
        private final long featureBranchId = 9202L;
        private final long userId = 1001L;

        private final ProjectCodeRepositoryRepository codeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
        private final ProjectBranchRepository branchRepository = mock(ProjectBranchRepository.class);
        private final ProjectWorkspaceRepository workspaceRepository = mock(ProjectWorkspaceRepository.class);
        private final ProjectWorkspaceItemRepository workspaceItemRepository = mock(ProjectWorkspaceItemRepository.class);
        private final ProjectBlobRepository blobRepository = mock(ProjectBlobRepository.class);
        private final ProjectCommitRepository commitRepository = mock(ProjectCommitRepository.class);
        private final ProjectCommitParentRepository commitParentRepository = mock(ProjectCommitParentRepository.class);
        private final ProjectSnapshotRepository snapshotRepository = mock(ProjectSnapshotRepository.class);
        private final ProjectSnapshotItemRepository snapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
        private final ProjectCommitChangeRepository commitChangeRepository = mock(ProjectCommitChangeRepository.class);
        private final ProjectFileRepository fileRepository = mock(ProjectFileRepository.class);
        private final ProjectFileVersionRepository fileVersionRepository = mock(ProjectFileVersionRepository.class);
        private final ProjectMergeRequestRepository mergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        private final ProjectReviewRepository reviewRepository = mock(ProjectReviewRepository.class);
        private final ProjectCheckRunRepository checkRunRepository = mock(ProjectCheckRunRepository.class);
        private final ProjectActivityLogRepository activityLogRepository = mock(ProjectActivityLogRepository.class);
        private final ProjectPermissionService permissionService = mock(ProjectPermissionService.class);
        private final ProjectRepositoryBootstrapSupport bootstrapSupport = mock(ProjectRepositoryBootstrapSupport.class);
        private final ProjectRepoStorageSupport repoStorageSupport = mock(ProjectRepoStorageSupport.class);
        private final FileStorageService fileStorageService = mock(FileStorageService.class);

        private final ProjectWorkspaceServiceImpl workspaceService = new ProjectWorkspaceServiceImpl(
                codeRepositoryRepository,
                branchRepository,
                workspaceRepository,
                workspaceItemRepository,
                blobRepository,
                repoStorageSupport,
                commitRepository,
                commitParentRepository,
                snapshotRepository,
                snapshotItemRepository,
                commitChangeRepository,
                fileRepository,
                fileVersionRepository,
                permissionService,
                bootstrapSupport
        );
        private final ProjectFileServiceImpl fileService = new ProjectFileServiceImpl(
                fileRepository,
                fileVersionRepository,
                codeRepositoryRepository,
                branchRepository,
                commitRepository,
                commitParentRepository,
                snapshotRepository,
                snapshotItemRepository,
                permissionService,
                fileStorageService,
                workspaceService,
                bootstrapSupport
        );
        private final ProjectMergeRequestServiceImpl mergeRequestService = new ProjectMergeRequestServiceImpl(
                codeRepositoryRepository,
                branchRepository,
                mergeRequestRepository,
                reviewRepository,
                checkRunRepository,
                activityLogRepository,
                commitRepository,
                commitParentRepository,
                snapshotRepository,
                snapshotItemRepository,
                commitChangeRepository,
                fileRepository,
                fileVersionRepository,
                blobRepository,
                permissionService,
                repoStorageSupport,
                new ObjectMapper()
        );

        private final Map<Long, ProjectCodeRepository> repositories = new LinkedHashMap<>();
        private final Map<Long, Long> repositoryIdByProject = new HashMap<>();
        private final Map<Long, ProjectBranch> branches = new LinkedHashMap<>();
        private final Map<Long, ProjectCommit> commits = new LinkedHashMap<>();
        private final Map<Long, List<ProjectCommitParent>> parentsByCommit = new HashMap<>();
        private final Map<Long, ProjectSnapshot> snapshots = new LinkedHashMap<>();
        private final Map<Long, List<ProjectSnapshotItem>> snapshotItemsBySnapshot = new HashMap<>();
        private final Map<Long, ProjectBlob> blobs = new LinkedHashMap<>();
        private final Map<Long, ProjectFile> files = new LinkedHashMap<>();
        private final Map<Long, ProjectFileVersion> versions = new LinkedHashMap<>();
        private final Map<Long, List<ProjectFileVersion>> versionsByFile = new HashMap<>();
        private final Map<Long, ProjectWorkspace> workspaces = new LinkedHashMap<>();
        private final Map<String, Long> activeWorkspaceIndex = new HashMap<>();
        private final Map<Long, ProjectWorkspaceItem> workspaceItems = new LinkedHashMap<>();
        private final Map<Long, Map<String, Long>> workspaceItemIndex = new HashMap<>();
        private final Map<Long, ProjectMergeRequest> mergeRequests = new LinkedHashMap<>();
        private final Map<Long, List<ProjectReview>> reviewsByMr = new HashMap<>();
        private final List<ProjectCheckRun> checkRuns = new ArrayList<>();
        private final List<ProjectActivityLog> activityLogs = new ArrayList<>();

        private final AtomicLong commitIdSeq = new AtomicLong(9500L);
        private final AtomicLong parentIdSeq = new AtomicLong(9600L);
        private final AtomicLong snapshotIdSeq = new AtomicLong(9700L);
        private final AtomicLong snapshotItemIdSeq = new AtomicLong(9800L);
        private final AtomicLong blobIdSeq = new AtomicLong(9900L);
        private final AtomicLong fileIdSeq = new AtomicLong(10000L);
        private final AtomicLong fileVersionIdSeq = new AtomicLong(10100L);
        private final AtomicLong workspaceIdSeq = new AtomicLong(10200L);
        private final AtomicLong workspaceItemIdSeq = new AtomicLong(10300L);
        private final AtomicLong mergeRequestIdSeq = new AtomicLong(10400L);
        private final AtomicLong checkRunIdSeq = new AtomicLong(10500L);
        private final AtomicLong activityLogIdSeq = new AtomicLong(10600L);

        private Long lastWorkspaceInsertId;
        private Long lastWorkspaceItemInsertId;

        private Harness() {
            wireRepositoryAnswers();
        }

        private void seedInitialGraph() {
            ProjectCodeRepository repository = ProjectCodeRepository.builder()
                    .id(repositoryId)
                    .projectId(projectId)
                    .defaultBranchId(mainBranchId)
                    .build();
            saveRepository(repository);

            ProjectBranch main = ProjectBranch.builder()
                    .id(mainBranchId)
                    .repositoryId(repositoryId)
                    .name("main")
                    .headCommitId(9302L)
                    .protectedFlag(false)
                    .allowDirectCommitFlag(true)
                    .build();
            ProjectBranch feature = ProjectBranch.builder()
                    .id(featureBranchId)
                    .repositoryId(repositoryId)
                    .name("feature/thread-4")
                    .headCommitId(9301L)
                    .protectedFlag(false)
                    .allowDirectCommitFlag(true)
                    .build();
            branches.put(main.getId(), main);
            branches.put(feature.getId(), feature);

            ProjectBlob baseBlob = ProjectBlob.builder()
                    .id(9401L)
                    .sha256("sha-base-app")
                    .sizeBytes(10L)
                    .mimeType("text/plain")
                    .storagePath("mem://blob/base-app")
                    .build();
            ProjectBlob targetBlob = ProjectBlob.builder()
                    .id(9402L)
                    .sha256("sha-target-app")
                    .sizeBytes(11L)
                    .mimeType("text/plain")
                    .storagePath("mem://blob/target-app")
                    .build();
            blobs.put(baseBlob.getId(), baseBlob);
            blobs.put(targetBlob.getId(), targetBlob);

            ProjectFile appFile = ProjectFile.builder()
                    .id(9501L)
                    .projectId(projectId)
                    .repositoryId(repositoryId)
                    .fileName("App.java")
                    .canonicalPath("/src/App.java")
                    .fileKey("/src/App.java")
                    .filePath(targetBlob.getStoragePath())
                    .fileSizeBytes(targetBlob.getSizeBytes())
                    .fileType("java")
                    .isMain(false)
                    .isLatest(true)
                    .deletedFlag(false)
                    .latestBlobId(targetBlob.getId())
                    .build();
            files.put(appFile.getId(), appFile);

            ProjectFileVersion baseVersion = ProjectFileVersion.builder()
                    .id(9601L)
                    .fileId(appFile.getId())
                    .repositoryId(repositoryId)
                    .commitId(9301L)
                    .blobId(baseBlob.getId())
                    .version("v1")
                    .versionSeq(1)
                    .contentHash(baseBlob.getSha256())
                    .pathAtVersion("/src/App.java")
                    .changeType("ADD")
                    .serverPath(baseBlob.getStoragePath())
                    .fileSizeBytes(baseBlob.getSizeBytes())
                    .uploadedAt(LocalDateTime.now().minusMinutes(5))
                    .build();
            ProjectFileVersion targetVersion = ProjectFileVersion.builder()
                    .id(9602L)
                    .fileId(appFile.getId())
                    .repositoryId(repositoryId)
                    .commitId(9302L)
                    .blobId(targetBlob.getId())
                    .version("v2")
                    .versionSeq(2)
                    .contentHash(targetBlob.getSha256())
                    .pathAtVersion("/src/App.java")
                    .changeType("MODIFY")
                    .parentVersionId(baseVersion.getId())
                    .serverPath(targetBlob.getStoragePath())
                    .fileSizeBytes(targetBlob.getSizeBytes())
                    .uploadedAt(LocalDateTime.now().minusMinutes(4))
                    .build();
            saveFileVersion(baseVersion);
            saveFileVersion(targetVersion);
            appFile.setLatestVersionId(targetVersion.getId());
            appFile.setLatestCommitId(9302L);

            ProjectCommit baseCommit = ProjectCommit.builder()
                    .id(9301L)
                    .repositoryId(repositoryId)
                    .branchId(mainBranchId)
                    .commitNo(1L)
                    .displaySha("base9301")
                    .message("base")
                    .commitType("normal")
                    .snapshotId(9201L)
                    .build();
            ProjectCommit targetCommit = ProjectCommit.builder()
                    .id(9302L)
                    .repositoryId(repositoryId)
                    .branchId(mainBranchId)
                    .commitNo(2L)
                    .displaySha("main9302")
                    .message("main update")
                    .commitType("normal")
                    .snapshotId(9202L)
                    .build();
            commits.put(baseCommit.getId(), baseCommit);
            commits.put(targetCommit.getId(), targetCommit);
            parentsByCommit.put(baseCommit.getId(), new ArrayList<>());
            parentsByCommit.put(targetCommit.getId(), new ArrayList<>(List.of(parent(targetCommit.getId(), baseCommit.getId(), 1))));

            snapshots.put(9201L, ProjectSnapshot.builder().id(9201L).repositoryId(repositoryId).commitId(9301L).fileCount(1).build());
            snapshots.put(9202L, ProjectSnapshot.builder().id(9202L).repositoryId(repositoryId).commitId(9302L).fileCount(1).build());
            snapshotItemsBySnapshot.put(9201L, new ArrayList<>(List.of(snapshotItem(9201L, appFile.getId(), baseVersion.getId(), baseBlob.getId(), "/src/App.java", baseBlob.getSha256()))));
            snapshotItemsBySnapshot.put(9202L, new ArrayList<>(List.of(snapshotItem(9202L, appFile.getId(), targetVersion.getId(), targetBlob.getId(), "/src/App.java", targetBlob.getSha256()))));

            repository.setHeadCommitId(targetCommit.getId());
            branches.put(main.getId(), main);
            branches.put(feature.getId(), feature);
        }

        private void wireRepositoryAnswers() {
            when(codeRepositoryRepository.findByProjectId(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(repositoryIdByProject.get(invocation.getArgument(0)))
                            .map(repositories::get));
            when(codeRepositoryRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(repositories.get(invocation.getArgument(0))));
            when(codeRepositoryRepository.save(any(ProjectCodeRepository.class))).thenAnswer(invocation ->
                    saveRepository(invocation.getArgument(0)));

            when(branchRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(branches.get(invocation.getArgument(0))));
            when(branchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> {
                ProjectBranch branch = invocation.getArgument(0);
                branches.put(branch.getId(), branch);
                return branch;
            });

            when(workspaceRepository.upsertActiveWorkspace(anyLong(), anyLong(), anyLong(), any())).thenAnswer(invocation -> {
                Long repoId = invocation.getArgument(0);
                Long branchId = invocation.getArgument(1);
                Long ownerId = invocation.getArgument(2);
                Long baseCommitId = invocation.getArgument(3);
                String key = workspaceKey(repoId, branchId, ownerId);
                Long id = activeWorkspaceIndex.get(key);
                if (id == null) {
                    id = workspaceIdSeq.incrementAndGet();
                    ProjectWorkspace workspace = ProjectWorkspace.builder()
                            .id(id)
                            .repositoryId(repoId)
                            .branchId(branchId)
                            .ownerId(ownerId)
                            .baseCommitId(baseCommitId)
                            .status("active")
                            .createdAt(LocalDateTime.now())
                            .updatedAt(LocalDateTime.now())
                            .build();
                    workspaces.put(id, workspace);
                    activeWorkspaceIndex.put(key, id);
                }
                lastWorkspaceInsertId = id;
                return 1;
            });
            when(workspaceRepository.selectLastInsertId()).thenAnswer(invocation -> lastWorkspaceInsertId);
            when(workspaceRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(workspaces.get(invocation.getArgument(0))));
            when(workspaceRepository.findFirstByRepositoryIdAndBranchIdAndOwnerIdAndStatusOrderByUpdatedAtDesc(anyLong(), anyLong(), anyLong(), any(String.class)))
                    .thenAnswer(invocation -> Optional.ofNullable(
                            workspaces.get(activeWorkspaceIndex.get(workspaceKey(invocation.getArgument(0), invocation.getArgument(1), invocation.getArgument(2))))
                    ));
            when(workspaceRepository.save(any(ProjectWorkspace.class))).thenAnswer(invocation -> {
                ProjectWorkspace workspace = invocation.getArgument(0);
                if (workspace.getId() == null) {
                    workspace.setId(workspaceIdSeq.incrementAndGet());
                    if (workspace.getCreatedAt() == null) {
                        workspace.setCreatedAt(LocalDateTime.now());
                    }
                }
                workspace.setUpdatedAt(LocalDateTime.now());
                workspaces.put(workspace.getId(), workspace);
                activeWorkspaceIndex.put(workspaceKey(workspace.getRepositoryId(), workspace.getBranchId(), workspace.getOwnerId()), workspace.getId());
                return workspace;
            });

            when(workspaceItemRepository.upsertByWorkspaceAndPath(anyLong(), any(String.class), any(), any(), any(String.class), any(), any(), any()))
                    .thenAnswer(invocation -> {
                        Long workspaceId = invocation.getArgument(0);
                        String canonicalPath = invocation.getArgument(1);
                        String tempStoragePath = invocation.getArgument(2);
                        Long blobId = invocation.getArgument(3);
                        String changeType = invocation.getArgument(4);
                        Boolean stagedFlag = invocation.getArgument(5);
                        Boolean conflictFlag = invocation.getArgument(6);
                        String detectedMessage = invocation.getArgument(7);

                        Map<String, Long> byPath = workspaceItemIndex.computeIfAbsent(workspaceId, ignored -> new HashMap<>());
                        Long existingId = byPath.get(canonicalPath);
                        ProjectWorkspaceItem item;
                        if (existingId == null) {
                            item = ProjectWorkspaceItem.builder()
                                    .id(workspaceItemIdSeq.incrementAndGet())
                                    .workspaceId(workspaceId)
                                    .canonicalPath(canonicalPath)
                                    .build();
                        } else {
                            item = workspaceItems.get(existingId);
                        }
                        item.setTempStoragePath(tempStoragePath);
                        item.setBlobId(blobId);
                        item.setChangeType(changeType);
                        item.setStagedFlag(stagedFlag);
                        item.setConflictFlag(conflictFlag);
                        item.setDetectedMessage(detectedMessage);
                        workspaceItems.put(item.getId(), item);
                        byPath.put(canonicalPath, item.getId());
                        lastWorkspaceItemInsertId = item.getId();
                        return 1;
                    });
            when(workspaceItemRepository.selectLastInsertId()).thenAnswer(invocation -> lastWorkspaceItemInsertId);
            when(workspaceItemRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(workspaceItems.get(invocation.getArgument(0))));
            when(workspaceItemRepository.findFirstByWorkspaceIdAndCanonicalPath(anyLong(), any(String.class))).thenAnswer(invocation -> {
                Long workspaceId = invocation.getArgument(0);
                String path = invocation.getArgument(1);
                Long id = workspaceItemIndex.getOrDefault(workspaceId, Map.of()).get(path);
                return Optional.ofNullable(id).map(workspaceItems::get);
            });
            when(workspaceItemRepository.findByWorkspaceIdOrderByIdAsc(anyLong())).thenAnswer(invocation -> workspaceItems.values().stream()
                    .filter(item -> Objects.equals(item.getWorkspaceId(), invocation.getArgument(0)))
                    .sorted(Comparator.comparing(ProjectWorkspaceItem::getId))
                    .toList());
            when(workspaceItemRepository.save(any(ProjectWorkspaceItem.class))).thenAnswer(invocation -> {
                ProjectWorkspaceItem item = invocation.getArgument(0);
                if (item.getId() == null) {
                    item.setId(workspaceItemIdSeq.incrementAndGet());
                }
                workspaceItems.put(item.getId(), item);
                workspaceItemIndex.computeIfAbsent(item.getWorkspaceId(), ignored -> new HashMap<>())
                        .put(item.getCanonicalPath(), item.getId());
                return item;
            });
            when(workspaceItemRepository.deleteByWorkspaceId(anyLong())).thenAnswer(invocation -> {
                Long workspaceId = invocation.getArgument(0);
                List<Long> ids = workspaceItems.values().stream()
                        .filter(item -> Objects.equals(item.getWorkspaceId(), workspaceId))
                        .map(ProjectWorkspaceItem::getId)
                        .toList();
                ids.forEach(workspaceItems::remove);
                workspaceItemIndex.remove(workspaceId);
                return (long) ids.size();
            });
            when(workspaceItemRepository.deleteByWorkspaceIdAndCanonicalPath(anyLong(), any(String.class))).thenAnswer(invocation -> {
                Long workspaceId = invocation.getArgument(0);
                String path = invocation.getArgument(1);
                Long id = workspaceItemIndex.getOrDefault(workspaceId, Map.of()).remove(path);
                if (id != null) {
                    workspaceItems.remove(id);
                    return 1L;
                }
                return 0L;
            });

            when(blobRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(blobs.get(invocation.getArgument(0))));
            when(blobRepository.save(any(ProjectBlob.class))).thenAnswer(invocation -> saveBlob(invocation.getArgument(0)));
            when(blobRepository.findBySha256AndSizeBytes(any(String.class), any(Long.class))).thenAnswer(invocation ->
                    blobs.values().stream()
                            .filter(blob -> Objects.equals(blob.getSha256(), invocation.getArgument(0)))
                            .filter(blob -> Objects.equals(blob.getSizeBytes(), invocation.getArgument(1)))
                            .findFirst());

            when(commitRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(commits.get(invocation.getArgument(0))));
            when(commitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(anyLong(), anyLong())).thenAnswer(invocation ->
                    commits.values().stream()
                            .filter(commit -> Objects.equals(commit.getRepositoryId(), invocation.getArgument(0)))
                            .filter(commit -> Objects.equals(commit.getBranchId(), invocation.getArgument(1)))
                            .max(Comparator.comparing(ProjectCommit::getCommitNo)));
            when(commitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
                ProjectCommit commit = invocation.getArgument(0);
                if (commit.getId() == null) {
                    commit.setId(commitIdSeq.incrementAndGet());
                }
                if (commit.getCreatedAt() == null) {
                    commit.setCreatedAt(LocalDateTime.now());
                }
                commits.put(commit.getId(), commit);
                return commit;
            });

            when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(anyLong())).thenAnswer(invocation ->
                    new ArrayList<>(parentsByCommit.getOrDefault(invocation.getArgument(0), List.of())));
            when(commitParentRepository.findByCommitIdIn(any(List.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                List<Long> commitIds = invocation.getArgument(0);
                List<ProjectCommitParent> result = new ArrayList<>();
                for (Long commitId : commitIds) {
                    result.addAll(parentsByCommit.getOrDefault(commitId, List.of()));
                }
                return result;
            });
            when(commitParentRepository.save(any(ProjectCommitParent.class))).thenAnswer(invocation -> {
                ProjectCommitParent parent = invocation.getArgument(0);
                if (parent.getId() == null) {
                    parent.setId(parentIdSeq.incrementAndGet());
                }
                parentsByCommit.computeIfAbsent(parent.getCommitId(), ignored -> new ArrayList<>()).add(parent);
                parentsByCommit.get(parent.getCommitId())
                        .sort(Comparator.comparing(ProjectCommitParent::getParentOrder));
                return parent;
            });

            when(snapshotRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(snapshots.get(invocation.getArgument(0))));
            when(snapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
                ProjectSnapshot snapshot = invocation.getArgument(0);
                if (snapshot.getId() == null) {
                    snapshot.setId(snapshotIdSeq.incrementAndGet());
                }
                snapshots.put(snapshot.getId(), snapshot);
                return snapshot;
            });

            when(snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(anyLong())).thenAnswer(invocation ->
                    snapshotItemsBySnapshot.getOrDefault(invocation.getArgument(0), List.of()).stream()
                            .sorted(Comparator.comparing(ProjectSnapshotItem::getCanonicalPath))
                            .toList());
            when(snapshotItemRepository.findBySnapshotIdAndProjectFileId(anyLong(), anyLong())).thenAnswer(invocation ->
                    snapshotItemsBySnapshot.getOrDefault(invocation.getArgument(0), List.of()).stream()
                            .filter(item -> Objects.equals(item.getProjectFileId(), invocation.getArgument(1)))
                            .findFirst());
            when(snapshotItemRepository.save(any(ProjectSnapshotItem.class))).thenAnswer(invocation -> {
                ProjectSnapshotItem item = invocation.getArgument(0);
                if (item.getId() == null) {
                    item.setId(snapshotItemIdSeq.incrementAndGet());
                }
                List<ProjectSnapshotItem> items = new ArrayList<>(snapshotItemsBySnapshot.getOrDefault(item.getSnapshotId(), new ArrayList<>()));
                items.removeIf(existing -> Objects.equals(existing.getCanonicalPath(), item.getCanonicalPath()));
                items.add(item);
                snapshotItemsBySnapshot.put(item.getSnapshotId(), items);
                return item;
            });

            when(fileRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(files.get(invocation.getArgument(0))));
            when(fileRepository.findByProjectIdAndCanonicalPathAndDeletedFlagFalse(anyLong(), any(String.class))).thenAnswer(invocation ->
                    files.values().stream()
                            .filter(file -> Objects.equals(file.getProjectId(), invocation.getArgument(0)))
                            .filter(file -> Objects.equals(file.getCanonicalPath(), invocation.getArgument(1)))
                            .filter(file -> !Boolean.TRUE.equals(file.getDeletedFlag()))
                            .findFirst());
            when(fileRepository.findByProjectIdAndIsMainTrue(anyLong())).thenAnswer(invocation ->
                    files.values().stream()
                            .filter(file -> Objects.equals(file.getProjectId(), invocation.getArgument(0)))
                            .filter(file -> Boolean.TRUE.equals(file.getIsMain()))
                            .toList());
            when(fileRepository.save(any(ProjectFile.class))).thenAnswer(invocation -> {
                ProjectFile file = invocation.getArgument(0);
                if (file.getId() == null) {
                    file.setId(fileIdSeq.incrementAndGet());
                }
                files.put(file.getId(), file);
                return file;
            });

            when(fileVersionRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(versions.get(invocation.getArgument(0))));
            when(fileVersionRepository.countByFileId(anyLong())).thenAnswer(invocation ->
                    (long) versionsByFile.getOrDefault(invocation.getArgument(0), List.of()).size());
            when(fileVersionRepository.findTopByFileIdOrderByUploadedAtDesc(anyLong())).thenAnswer(invocation ->
                    versionsByFile.getOrDefault(invocation.getArgument(0), List.of()).stream()
                            .max(Comparator.comparing(ProjectFileVersion::getUploadedAt, Comparator.nullsLast(LocalDateTime::compareTo))));
            when(fileVersionRepository.findByFileIdOrderByUploadedAtDesc(anyLong())).thenAnswer(invocation ->
                    versionsByFile.getOrDefault(invocation.getArgument(0), List.of()).stream()
                            .sorted(Comparator.comparing(ProjectFileVersion::getUploadedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                            .toList());
            when(fileVersionRepository.findByFileIdInOrderByUploadedAtDesc(any(List.class))).thenAnswer(invocation -> {
                @SuppressWarnings("unchecked")
                List<Long> fileIds = invocation.getArgument(0);
                List<ProjectFileVersion> result = new ArrayList<>();
                for (Long fileId : fileIds) {
                    result.addAll(versionsByFile.getOrDefault(fileId, List.of()));
                }
                result.sort(Comparator.comparing(ProjectFileVersion::getUploadedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed());
                return result;
            });
            when(fileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation ->
                    saveFileVersion(invocation.getArgument(0)));

            when(mergeRequestRepository.findById(anyLong())).thenAnswer(invocation ->
                    Optional.ofNullable(mergeRequests.get(invocation.getArgument(0))));
            when(mergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> {
                ProjectMergeRequest mr = invocation.getArgument(0);
                if (mr.getId() == null) {
                    mr.setId(mergeRequestIdSeq.incrementAndGet());
                }
                if (mr.getCreatedAt() == null) {
                    mr.setCreatedAt(LocalDateTime.now());
                }
                mergeRequests.put(mr.getId(), mr);
                return mr;
            });
            when(mergeRequestRepository.findByRepositoryIdOrderByCreatedAtDesc(anyLong())).thenAnswer(invocation ->
                    mergeRequests.values().stream()
                            .filter(mr -> Objects.equals(mr.getRepositoryId(), invocation.getArgument(0)))
                            .sorted(Comparator.comparing(ProjectMergeRequest::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                            .toList());
            when(mergeRequestRepository.findByRepositoryIdAndStatusOrderByCreatedAtDesc(anyLong(), any(String.class))).thenAnswer(invocation ->
                    mergeRequests.values().stream()
                            .filter(mr -> Objects.equals(mr.getRepositoryId(), invocation.getArgument(0)))
                            .filter(mr -> Objects.equals(mr.getStatus(), invocation.getArgument(1)))
                            .sorted(Comparator.comparing(ProjectMergeRequest::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo)).reversed())
                            .toList());

            when(reviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(anyLong())).thenAnswer(invocation ->
                    new ArrayList<>(reviewsByMr.getOrDefault(invocation.getArgument(0), List.of())));
            when(reviewRepository.save(any(ProjectReview.class))).thenAnswer(invocation -> {
                ProjectReview review = invocation.getArgument(0);
                if (review.getId() == null) {
                    review.setId(fileVersionIdSeq.incrementAndGet());
                }
                reviewsByMr.computeIfAbsent(review.getMergeRequestId(), ignored -> new ArrayList<>()).add(review);
                return review;
            });

            when(checkRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(anyLong())).thenAnswer(invocation ->
                    checkRuns.stream()
                            .filter(item -> Objects.equals(item.getMergeRequestId(), invocation.getArgument(0)))
                            .sorted(Comparator
                                    .comparing(ProjectCheckRun::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                    .thenComparing(ProjectCheckRun::getId, Comparator.nullsLast(Long::compareTo))
                                    .reversed())
                            .toList());
            when(checkRunRepository.findByCommitIdOrderByCreatedAtDesc(anyLong())).thenAnswer(invocation ->
                    checkRuns.stream()
                            .filter(item -> Objects.equals(item.getCommitId(), invocation.getArgument(0)))
                            .sorted(Comparator
                                    .comparing(ProjectCheckRun::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                    .thenComparing(ProjectCheckRun::getId, Comparator.nullsLast(Long::compareTo))
                                    .reversed())
                            .toList());
            when(checkRunRepository.save(any(ProjectCheckRun.class))).thenAnswer(invocation -> {
                ProjectCheckRun checkRun = invocation.getArgument(0);
                if (checkRun.getId() == null) {
                    checkRun.setId(checkRunIdSeq.incrementAndGet());
                }
                if (checkRun.getCreatedAt() == null) {
                    checkRun.setCreatedAt(LocalDateTime.now());
                }
                checkRuns.add(checkRun);
                return checkRun;
            });

            when(activityLogRepository.findTopByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(anyLong(), any(String.class)))
                    .thenAnswer(invocation -> activityLogs.stream()
                            .filter(log -> Objects.equals(log.getMergeRequestId(), invocation.getArgument(0)))
                            .filter(log -> Objects.equals(log.getAction(), invocation.getArgument(1)))
                            .max(Comparator
                                    .comparing(ProjectActivityLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                    .thenComparing(ProjectActivityLog::getId, Comparator.nullsLast(Long::compareTo))));
            when(activityLogRepository.findByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(anyLong(), any(String.class)))
                    .thenAnswer(invocation -> activityLogs.stream()
                            .filter(log -> Objects.equals(log.getMergeRequestId(), invocation.getArgument(0)))
                            .filter(log -> Objects.equals(log.getAction(), invocation.getArgument(1)))
                            .sorted(Comparator
                                    .comparing(ProjectActivityLog::getCreatedAt, Comparator.nullsLast(LocalDateTime::compareTo))
                                    .thenComparing(ProjectActivityLog::getId, Comparator.nullsLast(Long::compareTo))
                                    .reversed())
                            .toList());
            when(activityLogRepository.save(any(ProjectActivityLog.class))).thenAnswer(invocation -> {
                ProjectActivityLog log = invocation.getArgument(0);
                if (log.getId() == null) {
                    log.setId(activityLogIdSeq.incrementAndGet());
                }
                if (log.getCreatedAt() == null) {
                    log.setCreatedAt(LocalDateTime.now());
                }
                activityLogs.add(log);
                return log;
            });

            when(commitChangeRepository.save(any(ProjectCommitChange.class))).thenAnswer(invocation -> invocation.getArgument(0));

            when(repoStorageSupport.saveMultipart(any())).thenAnswer(invocation -> {
                var multipart = invocation.getArgument(0, org.springframework.web.multipart.MultipartFile.class);
                long id = blobIdSeq.incrementAndGet();
                ProjectBlob blob = ProjectBlob.builder()
                        .id(id)
                        .sha256("sha-upload-" + id)
                        .sizeBytes(multipart == null ? 0L : multipart.getSize())
                        .mimeType(multipart == null ? "application/octet-stream" : (multipart.getContentType() == null ? "application/octet-stream" : multipart.getContentType()))
                        .storagePath("mem://blob/" + id)
                        .build();
                blobs.put(blob.getId(), blob);
                return blob;
            });
            when(repoStorageSupport.saveTextContent(any(String.class), any(String.class))).thenAnswer(invocation -> {
                String content = invocation.getArgument(0);
                long id = blobIdSeq.incrementAndGet();
                ProjectBlob blob = ProjectBlob.builder()
                        .id(id)
                        .sha256("sha-text-" + id)
                        .sizeBytes(content == null ? 0L : (long) content.length())
                        .mimeType("text/plain")
                        .storagePath("mem://blob/" + id)
                        .build();
                blobs.put(blob.getId(), blob);
                return blob;
            });
        }

        private ProjectCodeRepository saveRepository(ProjectCodeRepository repository) {
            repositories.put(repository.getId(), repository);
            repositoryIdByProject.put(repository.getProjectId(), repository.getId());
            return repository;
        }

        private ProjectBlob saveBlob(ProjectBlob blob) {
            if (blob.getId() == null) {
                blob.setId(blobIdSeq.incrementAndGet());
            }
            blobs.put(blob.getId(), blob);
            return blob;
        }

        private ProjectFileVersion saveFileVersion(ProjectFileVersion version) {
            if (version.getId() == null) {
                version.setId(fileVersionIdSeq.incrementAndGet());
            }
            if (version.getUploadedAt() == null) {
                version.setUploadedAt(LocalDateTime.now());
            }
            versions.put(version.getId(), version);
            versionsByFile.computeIfAbsent(version.getFileId(), ignored -> new ArrayList<>()).add(version);
            versionsByFile.get(version.getFileId())
                    .sort(Comparator.comparing(ProjectFileVersion::getVersionSeq, Comparator.nullsLast(Integer::compareTo)));
            return version;
        }

        private ProjectSnapshotItem snapshotItem(Long snapshotId,
                                                 Long projectFileId,
                                                 Long versionId,
                                                 Long blobId,
                                                 String path,
                                                 String contentHash) {
            return ProjectSnapshotItem.builder()
                    .id(snapshotItemIdSeq.incrementAndGet())
                    .snapshotId(snapshotId)
                    .projectFileId(projectFileId)
                    .projectFileVersionId(versionId)
                    .blobId(blobId)
                    .canonicalPath(path)
                    .contentHash(contentHash)
                    .build();
        }

        private ProjectCommitParent parent(Long commitId, Long parentCommitId, int order) {
            return ProjectCommitParent.builder()
                    .id(parentIdSeq.incrementAndGet())
                    .commitId(commitId)
                    .parentCommitId(parentCommitId)
                    .parentOrder(order)
                    .build();
        }

        private String workspaceKey(Long repositoryId, Long branchId, Long ownerId) {
            return repositoryId + ":" + branchId + ":" + ownerId + ":active";
        }

        private ProjectBranch branch(long branchId) {
            return branches.get(branchId);
        }

        private ProjectCodeRepository repository() {
            return repositories.get(repositoryId);
        }
    }
}
