package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.dto.ContentConflictResolveRequest;
import com.alikeyou.itmoduleproject.support.diff.ContentConflictBlock;
import com.alikeyou.itmoduleproject.support.diff.ConflictDetail;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.ConflictType;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
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
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ProjectMergeRequestServiceImplTest {

    private static final ObjectMapper TEST_OBJECT_MAPPER = new ObjectMapper();

    private final ProjectActivityLogRepository activityLogRepository = mock(ProjectActivityLogRepository.class);
    private final ProjectCommitRepository commitRepository = mock(ProjectCommitRepository.class);
    private final ProjectCommitParentRepository commitParentRepository = mock(ProjectCommitParentRepository.class);
    private final ProjectMergeRequestServiceImpl service = new ProjectMergeRequestServiceImpl(
            mock(ProjectCodeRepositoryRepository.class),
            mock(ProjectBranchRepository.class),
            mock(ProjectMergeRequestRepository.class),
            mock(ProjectReviewRepository.class),
            mock(ProjectCheckRunRepository.class),
            activityLogRepository,
            commitRepository,
            commitParentRepository,
            mock(ProjectSnapshotRepository.class),
            mock(ProjectSnapshotItemRepository.class),
            mock(ProjectCommitChangeRepository.class),
            mock(ProjectFileRepository.class),
            mock(ProjectFileVersionRepository.class),
            mock(ProjectBlobRepository.class),
            mock(ProjectPermissionService.class),
            mock(ProjectRepoStorageSupport.class),
            new ObjectMapper()
    );

    @Test
    void resolveMergeBase_shouldPickNearestCommonAncestor() throws Exception {
        ProjectCommit ancestor = commit(2L);
        ProjectCommit sourceHead = commit(5L);
        ProjectCommit targetHead = commit(4L);

        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(4L))
                .thenReturn(List.of(parent(4L, 2L)));
        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(5L))
                .thenReturn(List.of(parent(5L, 3L)));
        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(3L))
                .thenReturn(List.of(parent(3L, 2L)));
        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(2L))
                .thenReturn(List.of(parent(2L, 1L)));
        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(1L))
                .thenReturn(List.of());
        when(commitRepository.findById(2L)).thenReturn(Optional.of(ancestor));

        Object mergeBase = invokePrivate(service, "resolveMergeBase",
                new Class[]{ProjectCommit.class, ProjectCommit.class},
                sourceHead, targetHead);

        assertInstanceOf(ProjectCommit.class, mergeBase);
        assertEquals(2L, ((ProjectCommit) mergeBase).getId());
    }

    @Test
    void buildThreeWayMergePlan_shouldAcceptSourceOnlyChange() throws Exception {
        Map<String, ProjectSnapshotItem> baseSnapshot = Map.of(
                "src/App.java", snapshotItem(1L, "src/App.java")
        );
        Map<String, ProjectSnapshotItem> sourceSnapshot = Map.of(
                "src/App.java", snapshotItem(2L, "src/App.java")
        );
        Map<String, ProjectSnapshotItem> targetSnapshot = Map.of(
                "src/App.java", snapshotItem(1L, "src/App.java")
        );

        Object mergePlan = invokePrivate(service, "buildThreeWayMergePlan",
                new Class[]{Map.class, Map.class, Map.class},
                baseSnapshot, sourceSnapshot, targetSnapshot);

        @SuppressWarnings("unchecked")
        List<String> conflictPaths = (List<String>) invokePrivate(mergePlan, "conflictPaths", new Class[]{});
        @SuppressWarnings("unchecked")
        List<Object> acceptedChanges = (List<Object>) invokePrivate(mergePlan, "acceptedChanges", new Class[]{});

        assertTrue(conflictPaths.isEmpty());
        assertEquals(1, acceptedChanges.size());
        assertEquals("src/App.java", invokePrivate(acceptedChanges.get(0), "path", new Class[]{}));
        assertEquals("MODIFY", invokePrivate(acceptedChanges.get(0), "changeType", new Class[]{}));
    }

    @Test
    void buildThreeWayMergePlan_shouldRejectDivergentChangesAsConflict() throws Exception {
        Map<String, ProjectSnapshotItem> baseSnapshot = Map.of(
                "src/App.java", snapshotItem(1L, "src/App.java")
        );
        Map<String, ProjectSnapshotItem> sourceSnapshot = Map.of(
                "src/App.java", snapshotItem(2L, "src/App.java")
        );
        Map<String, ProjectSnapshotItem> targetSnapshot = Map.of(
                "src/App.java", snapshotItem(3L, "src/App.java")
        );

        Object mergePlan = invokePrivate(service, "buildThreeWayMergePlan",
                new Class[]{Map.class, Map.class, Map.class},
                baseSnapshot, sourceSnapshot, targetSnapshot);

        @SuppressWarnings("unchecked")
        List<String> conflictPaths = (List<String>) invokePrivate(mergePlan, "conflictPaths", new Class[]{});
        @SuppressWarnings("unchecked")
        List<Object> acceptedChanges = (List<Object>) invokePrivate(mergePlan, "acceptedChanges", new Class[]{});

        assertEquals(List.of("src/App.java"), conflictPaths);
        assertTrue(acceptedChanges.isEmpty());
    }

    @Test
    void recordMergeRequestActivity_shouldPersistMergeRequestTraceLog() throws Exception {
        ProjectMergeRequest mergeRequest = ProjectMergeRequest.builder()
                .id(88L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .build();
        ProjectBranch branch = ProjectBranch.builder()
                .id(11L)
                .name("feature/thread4")
                .build();

        invokePrivate(service, "recordMergeRequestActivity",
                new Class[]{Long.class, Long.class, String.class, ProjectMergeRequest.class, ProjectBranch.class, Long.class, String.class, Map.class},
                99L, 1001L, "mr_merge", mergeRequest, branch, 501L, "Merged merge request", Map.of("mergeCommitId", 777L));

        ArgumentCaptor<ProjectActivityLog> captor = ArgumentCaptor.forClass(ProjectActivityLog.class);
        verify(activityLogRepository).save(captor.capture());
        ProjectActivityLog saved = captor.getValue();

        assertEquals(99L, saved.getProjectId());
        assertEquals(1001L, saved.getOperatorId());
        assertEquals("mr_merge", saved.getAction());
        assertEquals("merge_request", saved.getTargetType());
        assertEquals(88L, saved.getTargetId());
        assertEquals(88L, saved.getMergeRequestId());
        assertEquals(11L, saved.getBranchId());
        assertEquals(501L, saved.getCommitId());
        assertNotNull(saved.getDetails());
        assertTrue(saved.getDetails().contains("\"mergeCommitId\":777"));
    }

    @Test
    void applyHeadDriftState_shouldRequireRecheckWhenHeadChanged() throws Exception {
        Object mergeCheck = Class.forName("com.alikeyou.itmoduleproject.support.diff.MergeCheckResult")
                .getDeclaredConstructor()
                .newInstance();
        mergeCheck.getClass().getMethod("setSourceCommitId", Long.class).invoke(mergeCheck, 11L);
        mergeCheck.getClass().getMethod("setTargetCommitId", Long.class).invoke(mergeCheck, 21L);
        mergeCheck.getClass().getMethod("setMergeable", Boolean.class).invoke(mergeCheck, Boolean.TRUE);
        mergeCheck.getClass().getMethod("setBlockingReasons", List.class).invoke(mergeCheck, new java.util.ArrayList<>());
        mergeCheck.getClass().getMethod("setConflicts", List.class).invoke(mergeCheck, new java.util.ArrayList<>());
        mergeCheck.getClass().getMethod("setChanges", List.class).invoke(mergeCheck, new java.util.ArrayList<>());

        Object updated = invokePrivate(service, "applyHeadDriftState",
                new Class[]{mergeCheck.getClass(), Long.class, Long.class, boolean.class},
                mergeCheck, 11L, 22L, false);

        Boolean requiresRecheck = (Boolean) updated.getClass().getMethod("getRequiresRecheck").invoke(updated);
        Boolean mergeable = (Boolean) updated.getClass().getMethod("getMergeable").invoke(updated);
        @SuppressWarnings("unchecked")
        List<String> blockingReasons = (List<String>) updated.getClass().getMethod("getBlockingReasons").invoke(updated);
        @SuppressWarnings("unchecked")
        List<ConflictDetail> conflicts = (List<ConflictDetail>) updated.getClass().getMethod("getConflicts").invoke(updated);

        assertTrue(Boolean.TRUE.equals(requiresRecheck));
        assertFalse(Boolean.TRUE.equals(mergeable));
        assertTrue(blockingReasons.contains("RECHECK_REQUIRED"));
        assertEquals(1, conflicts.size());
        ConflictDetail staleConflict = conflicts.get(0);
        assertEquals(ConflictType.STALE_BRANCH, staleConflict.getConflictType());
        assertEquals(Boolean.FALSE, staleConflict.getBinaryFile());
        assertEquals(Boolean.TRUE, staleConflict.getMetadata().get("requiresRecheck"));
        assertEquals(Boolean.FALSE, staleConflict.getMetadata().get("requiresBranchUpdate"));
    }

    @Test
    void applyStaleBranchRequirement_shouldAddBranchUpdateConflict() throws Exception {
        MergeCheckResult mergeCheck = MergeCheckResult.builder()
                .sourceCommitId(11L)
                .targetCommitId(22L)
                .mergeable(Boolean.TRUE)
                .blockingReasons(new java.util.ArrayList<>())
                .conflicts(new java.util.ArrayList<>())
                .changes(new java.util.ArrayList<>())
                .build();
        when(commitParentRepository.findByCommitIdOrderByParentOrderAsc(11L)).thenReturn(List.of());

        MergeCheckResult updated = (MergeCheckResult) invokePrivate(
                service,
                "applyStaleBranchRequirement",
                new Class[]{MergeCheckResult.class, Long.class, Long.class},
                mergeCheck,
                11L,
                22L
        );

        assertTrue(Boolean.TRUE.equals(updated.getRequiresBranchUpdate()));
        assertFalse(Boolean.TRUE.equals(updated.getMergeable()));
        assertTrue(updated.getBlockingReasons().contains("BRANCH_UPDATE_REQUIRED"));
        assertEquals(1, updated.getConflicts().size());
        ConflictDetail conflict = updated.getConflicts().get(0);
        assertEquals(ConflictType.STALE_BRANCH, conflict.getConflictType());
        assertEquals(Boolean.TRUE, conflict.getMetadata().get("requiresBranchUpdate"));
        assertEquals(Boolean.FALSE, conflict.getMetadata().get("requiresRecheck"));
    }

    @Test
    void updateSourceBranchWithTarget_shouldCreateSourceMergeCommitAndKeepSourceHistoryReachable() throws Exception {
        ProjectActivityLogRepository localActivityLogRepository = mock(ProjectActivityLogRepository.class);
        ProjectCommitRepository localCommitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository localCommitParentRepository = mock(ProjectCommitParentRepository.class);
        ProjectCodeRepositoryRepository localCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
        ProjectBranchRepository localBranchRepository = mock(ProjectBranchRepository.class);
        ProjectMergeRequestRepository localMergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectSnapshotRepository localSnapshotRepository = mock(ProjectSnapshotRepository.class);
        ProjectSnapshotItemRepository localSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
        ProjectCommitChangeRepository localCommitChangeRepository = mock(ProjectCommitChangeRepository.class);
        ProjectFileRepository localFileRepository = mock(ProjectFileRepository.class);
        ProjectFileVersionRepository localFileVersionRepository = mock(ProjectFileVersionRepository.class);
        ProjectBlobRepository localBlobRepository = mock(ProjectBlobRepository.class);
        ProjectPermissionService localPermissionService = mock(ProjectPermissionService.class);
        ProjectRepoStorageSupport localStorageSupport = mock(ProjectRepoStorageSupport.class);

        ProjectMergeRequestServiceImpl localService = new ProjectMergeRequestServiceImpl(
                localCodeRepositoryRepository,
                localBranchRepository,
                localMergeRequestRepository,
                localReviewRepository,
                localCheckRunRepository,
                localActivityLogRepository,
                localCommitRepository,
                localCommitParentRepository,
                localSnapshotRepository,
                localSnapshotItemRepository,
                localCommitChangeRepository,
                localFileRepository,
                localFileVersionRepository,
                localBlobRepository,
                localPermissionService,
                localStorageSupport,
                new ObjectMapper()
        );

        ProjectCodeRepository repo = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(9L)
                .defaultBranchId(12L)
                .headCommitId(30L)
                .build();
        ProjectBranch source = ProjectBranch.builder()
                .id(11L)
                .repositoryId(100L)
                .name("dev")
                .headCommitId(20L)
                .build();
        ProjectBranch target = ProjectBranch.builder()
                .id(12L)
                .repositoryId(100L)
                .name("main")
                .headCommitId(30L)
                .build();
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .sourceHeadCommitId(20L)
                .targetHeadCommitId(30L)
                .build();
        ProjectCommit sourceHead = ProjectCommit.builder()
                .id(20L)
                .repositoryId(100L)
                .branchId(11L)
                .commitNo(2L)
                .snapshotId(200L)
                .build();
        ProjectCommit targetHead = ProjectCommit.builder()
                .id(30L)
                .repositoryId(100L)
                .branchId(12L)
                .commitNo(3L)
                .snapshotId(300L)
                .build();
        ProjectCommit mergeBase = ProjectCommit.builder()
                .id(10L)
                .repositoryId(100L)
                .branchId(12L)
                .commitNo(1L)
                .snapshotId(100L)
                .build();

        Object context = newMergeCheckContext(mr, repo, source, target);
        Object executionState = newMergeExecutionState(
                sourceHead,
                targetHead,
                mergeBase,
                Map.of("base.txt", snapshotItem(1L, "base.txt")),
                Map.of(
                        "base.txt", snapshotItem(1L, "base.txt"),
                        "dev.txt", snapshotItem(2L, "dev.txt")
                ),
                Map.of(
                        "base.txt", snapshotItem(1L, "base.txt"),
                        "target.txt", snapshotItem(3L, "target.txt")
                )
        );

        when(localBranchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(localBranchRepository.findById(12L)).thenReturn(Optional.of(target));
        when(localCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(100L, 11L))
                .thenReturn(Optional.of(sourceHead));
        when(localCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
            ProjectCommit commit = invocation.getArgument(0);
            if (commit.getId() == null) {
                commit.setId(40L);
            }
            return commit;
        });
        when(localSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.getId() == null) {
                snapshot.setId(400L);
            }
            return snapshot;
        });
        when(localSnapshotItemRepository.save(any(ProjectSnapshotItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        ProjectBlob targetBlob = ProjectBlob.builder()
                .id(3L)
                .sha256("hash-3")
                .storagePath("/tmp/target.txt")
                .sizeBytes(12L)
                .mimeType("text/plain")
                .build();
        ProjectFile targetFile = ProjectFile.builder()
                .id(3L)
                .projectId(9L)
                .repositoryId(100L)
                .fileName("target.txt")
                .canonicalPath("target.txt")
                .latestVersionId(3L)
                .build();
        when(localBlobRepository.findById(3L)).thenReturn(Optional.of(targetBlob));
        when(localFileRepository.findById(3L)).thenReturn(Optional.of(targetFile));
        when(localFileRepository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localFileVersionRepository.countByFileId(3L)).thenReturn(1L);
        when(localFileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation -> {
            ProjectFileVersion version = invocation.getArgument(0);
            version.setId(30L);
            return version;
        });
        when(localCommitChangeRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(localBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localMergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object outcome = invokePrivate(
                localService,
                "updateSourceBranchWithTarget",
                new Class[]{context.getClass(), executionState.getClass(), Long.class},
                context,
                executionState,
                1001L
        );

        assertEquals(40L, invokePrivate(outcome, "createdCommitId", new Class[]{}));
        assertEquals(40L, source.getHeadCommitId());
        assertEquals(40L, mr.getSourceHeadCommitId());
        assertEquals(30L, target.getHeadCommitId());
        assertEquals(30L, repo.getHeadCommitId());
        verify(localBranchRepository).save(source);
        verify(localMergeRequestRepository).save(mr);

        ArgumentCaptor<ProjectCommit> commitCaptor = ArgumentCaptor.forClass(ProjectCommit.class);
        verify(localCommitRepository, org.mockito.Mockito.atLeastOnce()).save(commitCaptor.capture());
        ProjectCommit updateCommit = commitCaptor.getAllValues().get(0);
        assertEquals(11L, updateCommit.getBranchId());
        assertEquals("merge", updateCommit.getCommitType());
        assertEquals(Boolean.TRUE, updateCommit.getIsMergeCommit());

        ArgumentCaptor<ProjectCommitParent> parentCaptor = ArgumentCaptor.forClass(ProjectCommitParent.class);
        verify(localCommitParentRepository, org.mockito.Mockito.times(2)).save(parentCaptor.capture());
        assertEquals(20L, parentCaptor.getAllValues().get(0).getParentCommitId());
        assertEquals(1, parentCaptor.getAllValues().get(0).getParentOrder());
        assertEquals(30L, parentCaptor.getAllValues().get(1).getParentCommitId());
        assertEquals(2, parentCaptor.getAllValues().get(1).getParentOrder());

        ArgumentCaptor<ProjectSnapshotItem> itemCaptor = ArgumentCaptor.forClass(ProjectSnapshotItem.class);
        verify(localSnapshotItemRepository, org.mockito.Mockito.times(3)).save(itemCaptor.capture());
        List<String> savedPaths = itemCaptor.getAllValues().stream()
                .map(ProjectSnapshotItem::getCanonicalPath)
                .toList();
        assertTrue(savedPaths.contains("base.txt"));
        assertTrue(savedPaths.contains("dev.txt"));
        assertTrue(savedPaths.contains("target.txt"));
    }

    @Test
    void updateSourceBranchWithTarget_shouldReturnContentConflictWithoutMovingSourceHead() throws Exception {
        ProjectBranchRepository localBranchRepository = mock(ProjectBranchRepository.class);
        ProjectCommitRepository localCommitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository localCommitParentRepository = mock(ProjectCommitParentRepository.class);
        ProjectMergeRequestServiceImpl localService = new ProjectMergeRequestServiceImpl(
                mock(ProjectCodeRepositoryRepository.class),
                localBranchRepository,
                mock(ProjectMergeRequestRepository.class),
                mock(ProjectReviewRepository.class),
                mock(ProjectCheckRunRepository.class),
                mock(ProjectActivityLogRepository.class),
                localCommitRepository,
                localCommitParentRepository,
                mock(ProjectSnapshotRepository.class),
                mock(ProjectSnapshotItemRepository.class),
                mock(ProjectCommitChangeRepository.class),
                mock(ProjectFileRepository.class),
                mock(ProjectFileVersionRepository.class),
                mock(ProjectBlobRepository.class),
                mock(ProjectPermissionService.class),
                mock(ProjectRepoStorageSupport.class),
                TEST_OBJECT_MAPPER
        );

        ProjectCodeRepository repo = ProjectCodeRepository.builder().id(100L).projectId(9L).defaultBranchId(12L).headCommitId(30L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).repositoryId(100L).name("dev").headCommitId(20L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).repositoryId(100L).name("main").headCommitId(30L).build();
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .sourceHeadCommitId(20L)
                .targetHeadCommitId(30L)
                .build();
        ProjectCommit sourceHead = ProjectCommit.builder().id(20L).repositoryId(100L).branchId(11L).snapshotId(200L).build();
        ProjectCommit targetHead = ProjectCommit.builder().id(30L).repositoryId(100L).branchId(12L).snapshotId(300L).build();
        ProjectCommit mergeBase = ProjectCommit.builder().id(10L).repositoryId(100L).branchId(12L).snapshotId(100L).build();

        Object context = newMergeCheckContext(mr, repo, source, target);
        Object executionState = newMergeExecutionState(
                sourceHead,
                targetHead,
                mergeBase,
                Map.of("src/App.java", snapshotItem(1L, "src/App.java")),
                Map.of("src/App.java", snapshotItem(2L, "src/App.java")),
                Map.of("src/App.java", snapshotItem(3L, "src/App.java"))
        );
        when(localCommitParentRepository.findByCommitIdOrderByParentOrderAsc(20L)).thenReturn(List.of(parent(20L, 10L)));
        when(localCommitParentRepository.findByCommitIdOrderByParentOrderAsc(10L)).thenReturn(List.of());

        Object outcome = invokePrivate(
                localService,
                "updateSourceBranchWithTarget",
                new Class[]{context.getClass(), executionState.getClass(), Long.class},
                context,
                executionState,
                1001L
        );

        assertNull(invokePrivate(outcome, "createdCommitId", new Class[]{}));
        MergeCheckResult result = (MergeCheckResult) invokePrivate(outcome, "mergeCheckResult", new Class[]{});
        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(Boolean.TRUE.equals(result.getRequiresBranchUpdate()));
        assertTrue(result.getConflicts().stream().anyMatch(conflict -> ConflictType.CONTENT_CONFLICT.equals(conflict.getConflictType())));
        assertTrue(result.getConflicts().stream().anyMatch(conflict -> ConflictType.STALE_BRANCH.equals(conflict.getConflictType())));
        assertEquals(20L, source.getHeadCommitId());
        assertEquals(20L, mr.getSourceHeadCommitId());
        verify(localCommitRepository, never()).save(any(ProjectCommit.class));
        verify(localBranchRepository, never()).save(any(ProjectBranch.class));
    }

    @Test
    void updateSourceBranchWithTarget_shouldAllowPreviouslyResolvedContentConflictFromSupplementalCommit() throws Exception {
        ProjectActivityLogRepository localActivityLogRepository = mock(ProjectActivityLogRepository.class);
        ProjectCommitRepository localCommitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository localCommitParentRepository = mock(ProjectCommitParentRepository.class);
        ProjectCodeRepositoryRepository localCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
        ProjectBranchRepository localBranchRepository = mock(ProjectBranchRepository.class);
        ProjectMergeRequestRepository localMergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectSnapshotRepository localSnapshotRepository = mock(ProjectSnapshotRepository.class);
        ProjectSnapshotItemRepository localSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
        ProjectCommitChangeRepository localCommitChangeRepository = mock(ProjectCommitChangeRepository.class);
        ProjectFileRepository localFileRepository = mock(ProjectFileRepository.class);
        ProjectFileVersionRepository localFileVersionRepository = mock(ProjectFileVersionRepository.class);
        ProjectBlobRepository localBlobRepository = mock(ProjectBlobRepository.class);
        ProjectPermissionService localPermissionService = mock(ProjectPermissionService.class);
        ProjectRepoStorageSupport localStorageSupport = mock(ProjectRepoStorageSupport.class);

        ProjectMergeRequestServiceImpl localService = new ProjectMergeRequestServiceImpl(
                localCodeRepositoryRepository,
                localBranchRepository,
                localMergeRequestRepository,
                localReviewRepository,
                localCheckRunRepository,
                localActivityLogRepository,
                localCommitRepository,
                localCommitParentRepository,
                localSnapshotRepository,
                localSnapshotItemRepository,
                localCommitChangeRepository,
                localFileRepository,
                localFileVersionRepository,
                localBlobRepository,
                localPermissionService,
                localStorageSupport,
                new ObjectMapper()
        );

        ProjectCodeRepository repo = ProjectCodeRepository.builder().id(100L).projectId(9L).defaultBranchId(12L).headCommitId(30L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).repositoryId(100L).name("dev").headCommitId(40L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).repositoryId(100L).name("main").headCommitId(30L).build();
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .sourceHeadCommitId(40L)
                .targetHeadCommitId(30L)
                .build();
        ProjectCommit sourceHead = ProjectCommit.builder().id(40L).repositoryId(100L).branchId(11L).commitNo(4L).snapshotId(400L).build();
        ProjectCommit targetHead = ProjectCommit.builder().id(30L).repositoryId(100L).branchId(12L).commitNo(3L).snapshotId(300L).build();
        ProjectCommit mergeBase = ProjectCommit.builder().id(10L).repositoryId(100L).branchId(12L).commitNo(1L).snapshotId(100L).build();

        Object context = newMergeCheckContext(mr, repo, source, target);
        Object executionState = newMergeExecutionState(
                sourceHead,
                targetHead,
                mergeBase,
                Map.of("src/App.java", snapshotItem(1L, "src/App.java")),
                Map.of("src/App.java", snapshotItem(4L, "src/App.java")),
                Map.of("src/App.java", snapshotItem(3L, "src/App.java"))
        );

        ProjectActivityLog resolutionLog = ProjectActivityLog.builder()
                .id(901L)
                .mergeRequestId(88L)
                .commitId(40L)
                .action("mr_conflict_resolve")
                .details("{\"resolutionMode\":\"SUPPLEMENTAL_COMMIT\",\"createdCommitId\":40,\"resolvedPath\":\"src/App.java\",\"conflicts\":[{\"strategy\":\"MANUAL_CONTENT\",\"path\":\"src/App.java\"}]}")
                .build();

        when(localActivityLogRepository.findByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(88L, "mr_conflict_resolve"))
                .thenReturn(List.of(resolutionLog));
        when(localCommitParentRepository.findByCommitIdOrderByParentOrderAsc(40L)).thenReturn(List.of(parent(40L, 20L)));
        when(localCommitParentRepository.findByCommitIdOrderByParentOrderAsc(20L)).thenReturn(List.of(parent(20L, 10L)));
        when(localCommitParentRepository.findByCommitIdOrderByParentOrderAsc(10L)).thenReturn(List.of());
        when(localBranchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(localBranchRepository.findById(12L)).thenReturn(Optional.of(target));
        when(localCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(100L, 11L))
                .thenReturn(Optional.of(sourceHead));
        when(localCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
            ProjectCommit commit = invocation.getArgument(0);
            if (commit.getId() == null) {
                commit.setId(50L);
            }
            return commit;
        });
        when(localSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.getId() == null) {
                snapshot.setId(500L);
            }
            return snapshot;
        });
        when(localSnapshotItemRepository.save(any(ProjectSnapshotItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localMergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Object outcome = invokePrivate(
                localService,
                "updateSourceBranchWithTarget",
                new Class[]{context.getClass(), executionState.getClass(), Long.class},
                context,
                executionState,
                1001L
        );

        assertEquals(50L, invokePrivate(outcome, "createdCommitId", new Class[]{}));
        assertEquals(50L, source.getHeadCommitId());
        assertEquals(50L, mr.getSourceHeadCommitId());

        ArgumentCaptor<ProjectCommitParent> parentCaptor = ArgumentCaptor.forClass(ProjectCommitParent.class);
        verify(localCommitParentRepository, org.mockito.Mockito.times(2)).save(parentCaptor.capture());
        assertEquals(40L, parentCaptor.getAllValues().get(0).getParentCommitId());
        assertEquals(30L, parentCaptor.getAllValues().get(1).getParentCommitId());
        ArgumentCaptor<ProjectSnapshotItem> itemCaptor = ArgumentCaptor.forClass(ProjectSnapshotItem.class);
        verify(localSnapshotItemRepository).save(itemCaptor.capture());
        assertEquals("src/App.java", itemCaptor.getValue().getCanonicalPath());
    }

    @Test
    void merge_shouldCompleteWithoutCommitWhenSourceHeadAlreadyEqualsTargetHead() {
        ProjectActivityLogRepository localActivityLogRepository = mock(ProjectActivityLogRepository.class);
        ProjectCommitRepository localCommitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository localCommitParentRepository = mock(ProjectCommitParentRepository.class);
        ProjectCodeRepositoryRepository localCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
        ProjectBranchRepository localBranchRepository = mock(ProjectBranchRepository.class);
        ProjectMergeRequestRepository localMergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectMergeRequestServiceImpl localService = new ProjectMergeRequestServiceImpl(
                localCodeRepositoryRepository,
                localBranchRepository,
                localMergeRequestRepository,
                localReviewRepository,
                localCheckRunRepository,
                localActivityLogRepository,
                localCommitRepository,
                localCommitParentRepository,
                mock(ProjectSnapshotRepository.class),
                mock(ProjectSnapshotItemRepository.class),
                mock(ProjectCommitChangeRepository.class),
                mock(ProjectFileRepository.class),
                mock(ProjectFileVersionRepository.class),
                mock(ProjectBlobRepository.class),
                mock(ProjectPermissionService.class),
                mock(ProjectRepoStorageSupport.class),
                TEST_OBJECT_MAPPER
        );

        ProjectMergeRequest mergeRequest = ProjectMergeRequest.builder()
                .id(22L)
                .repositoryId(8L)
                .sourceBranchId(17L)
                .targetBranchId(16L)
                .sourceHeadCommitId(77L)
                .targetHeadCommitId(77L)
                .status("open")
                .title("Merge dev -> main")
                .createdBy(1L)
                .build();
        ProjectCodeRepository repo = ProjectCodeRepository.builder()
                .id(8L)
                .projectId(9L)
                .defaultBranchId(16L)
                .headCommitId(77L)
                .build();
        ProjectBranch source = ProjectBranch.builder()
                .id(17L)
                .repositoryId(8L)
                .name("dev")
                .headCommitId(77L)
                .protectedFlag(false)
                .build();
        ProjectBranch target = ProjectBranch.builder()
                .id(16L)
                .repositoryId(8L)
                .name("main")
                .headCommitId(77L)
                .protectedFlag(false)
                .build();
        ProjectCommit sharedHead = ProjectCommit.builder()
                .id(77L)
                .repositoryId(8L)
                .branchId(16L)
                .commitNo(2L)
                .snapshotId(77L)
                .build();

        when(localMergeRequestRepository.findById(22L)).thenReturn(Optional.of(mergeRequest));
        when(localMergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localCodeRepositoryRepository.findById(8L)).thenReturn(Optional.of(repo));
        when(localBranchRepository.findById(17L)).thenReturn(Optional.of(source));
        when(localBranchRepository.findById(16L)).thenReturn(Optional.of(target));
        when(localCommitRepository.findById(77L)).thenReturn(Optional.of(sharedHead));
        when(localReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(22L)).thenReturn(List.of());
        when(localCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(22L)).thenReturn(List.of());

        var result = localService.merge(22L, 1L);

        assertEquals("merged", result.getStatus());
        assertEquals(77L, result.getSourceHeadCommitId());
        assertEquals(77L, result.getTargetHeadCommitId());
        assertEquals(1L, result.getMergedBy());
        assertNotNull(result.getMergedAt());
        verify(localCommitRepository, never()).save(any(ProjectCommit.class));
        verify(localCommitParentRepository, never()).save(any(ProjectCommitParent.class));
    }

    @Test
    void validateConflictResolutionOptions_shouldRejectConflictOutsideLatestResult() throws Exception {
        ConflictResolutionOption option = ConflictResolutionOption.builder()
                .conflictId("missing-conflict-id")
                .resolutionStrategy("KEEP_TARGET")
                .build();

        BusinessExceptionWrapper thrown = assertThrows(BusinessExceptionWrapper.class, () -> invokePrivateWithBusinessException(
                service,
                "validateConflictResolutionOptions",
                new Class[]{List.class, Map.class},
                List.of(option),
                Map.of()
        ));
        assertTrue(thrown.getMessage().contains("does not belong to latest merge-check result"));
    }

    @Test
    void buildResolvedEquivalentResult_shouldDropResolvedStructuredConflict() throws Exception {
        ConflictDetail conflict = ConflictDetail.builder()
                .conflictId("c1")
                .conflictType(ConflictType.DELETE_MODIFY_CONFLICT)
                .summary("Delete/modify conflict")
                .build();
        MergeCheckResult latest = MergeCheckResult.builder()
                .mergeRequestId(1L)
                .sourceCommitId(11L)
                .targetCommitId(22L)
                .mergeable(false)
                .conflicts(new java.util.ArrayList<>(List.of(conflict)))
                .blockingReasons(new java.util.ArrayList<>(List.of("UNRESOLVED_CONFLICTS")))
                .build();
        ConflictResolutionOption option = ConflictResolutionOption.builder()
                .conflictId("c1")
                .resolutionStrategy("KEEP_TARGET")
                .build();

        @SuppressWarnings("unchecked")
        MergeCheckResult resolved = (MergeCheckResult) invokePrivate(service,
                "buildResolvedEquivalentResult",
                new Class[]{MergeCheckResult.class, List.class, Map.class},
                latest,
                List.of(option),
                Map.of("c1", conflict));

        assertTrue(resolved.getConflicts().isEmpty());
        assertTrue(Boolean.TRUE.equals(resolved.getMergeable()));
        assertFalse(resolved.getBlockingReasons().contains("UNRESOLVED_CONFLICTS"));
    }

    @Test
    void buildConflictResolutionOptionTrace_shouldCarryConflictFields() throws Exception {
        ConflictResolutionOption option = ConflictResolutionOption.builder()
                .conflictId("c1")
                .resolutionStrategy("KEEP_TARGET")
                .targetPath("docs/merged.md")
                .note("prefer target branch location")
                .build();
        ConflictDetail conflict = ConflictDetail.builder()
                .conflictId("c1")
                .conflictType(ConflictType.DELETE_MODIFY_CONFLICT)
                .build();

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> trace = (List<Map<String, Object>>) invokePrivate(
                service,
                "buildConflictResolutionOptionTrace",
                new Class[]{List.class, Map.class},
                List.of(option),
                Map.of("c1", conflict)
        );

        assertEquals(1, trace.size());
        assertEquals("c1", trace.get(0).get("conflictId"));
        assertEquals("DELETE_MODIFY_CONFLICT", trace.get(0).get("conflictType"));
        assertEquals("KEEP_TARGET", trace.get(0).get("strategy"));
        assertEquals("docs/merged.md", trace.get(0).get("targetPath"));
        assertEquals("prefer target branch location", trace.get(0).get("note"));
    }

    @Test
    void buildConflictResolutionTraceDetails_shouldIncludeRequiredTrackingFields() throws Exception {
        MergeCheckResult recheckResult = MergeCheckResult.builder()
                .sourceCommitId(11L)
                .targetCommitId(22L)
                .baseCommitId(9L)
                .mergeable(Boolean.FALSE)
                .requiresRecheck(Boolean.FALSE)
                .requiresBranchUpdate(Boolean.FALSE)
                .summary("still blocked")
                .blockingReasons(new java.util.ArrayList<>(List.of("UNRESOLVED_CONFLICTS")))
                .conflicts(new java.util.ArrayList<>(List.of(
                        ConflictDetail.builder().conflictId("c1").conflictType(ConflictType.DELETE_MODIFY_CONFLICT).build()
                )))
                .build();

        @SuppressWarnings("unchecked")
        Map<String, Object> details = (Map<String, Object>) invokePrivate(
                service,
                "buildConflictResolutionTraceDetails",
                new Class[]{Long.class, List.class, Long.class, Long.class, MergeCheckResult.class},
                88L,
                List.of(Map.of(
                        "conflictId", "c1",
                        "conflictType", "DELETE_MODIFY_CONFLICT",
                        "strategy", "KEEP_TARGET"
                )),
                1001L,
                null,
                recheckResult
        );

        assertEquals(88L, details.get("mergeRequestId"));
        assertEquals(1001L, details.get("operator"));
        assertTrue(details.containsKey("createdCommitId"));
        assertTrue(details.containsKey("conflicts"));
        assertTrue(details.containsKey("recheckResult"));
        @SuppressWarnings("unchecked")
        Map<String, Object> recheck = (Map<String, Object>) details.get("recheckResult");
        assertEquals(Boolean.FALSE, recheck.get("mergeable"));
        assertEquals(1, recheck.get("conflictCount"));
        assertEquals(11L, recheck.get("sourceCommitId"));
        assertEquals(22L, recheck.get("targetCommitId"));
        assertEquals(9L, recheck.get("baseCommitId"));
        assertEquals(recheckResult, details.get("latestMergeCheck"));
    }

    @Test
    void selectLatestEffectiveMergeCheck_shouldPreferNewerStructuredResolutionResult() throws Exception {
        MergeCheckResult mergeCheckResult = MergeCheckResult.builder()
                .mergeRequestId(88L)
                .sourceCommitId(11L)
                .targetCommitId(22L)
                .summary("raw merge check")
                .mergeable(Boolean.FALSE)
                .blockingReasons(new java.util.ArrayList<>(List.of("UNRESOLVED_CONFLICTS")))
                .conflicts(new java.util.ArrayList<>(List.of(
                        ConflictDetail.builder().conflictId("c1").conflictType(ConflictType.DELETE_MODIFY_CONFLICT).build()
                )))
                .build();
        MergeCheckResult resolvedEquivalentResult = MergeCheckResult.builder()
                .mergeRequestId(88L)
                .sourceCommitId(11L)
                .targetCommitId(22L)
                .summary("resolved after structured apply")
                .mergeable(Boolean.TRUE)
                .blockingReasons(new java.util.ArrayList<>())
                .conflicts(new java.util.ArrayList<>())
                .build();
        ProjectActivityLog mergeCheckLog = ProjectActivityLog.builder()
                .id(101L)
                .createdAt(LocalDateTime.of(2026, 4, 14, 20, 0, 0))
                .details(TEST_OBJECT_MAPPER.writeValueAsString(mergeCheckResult))
                .build();
        ProjectActivityLog resolutionLog = ProjectActivityLog.builder()
                .id(102L)
                .createdAt(LocalDateTime.of(2026, 4, 14, 20, 5, 0))
                .details(TEST_OBJECT_MAPPER.writeValueAsString(Map.of(
                        "latestMergeCheck", resolvedEquivalentResult
                )))
                .build();

        MergeCheckResult selected = (MergeCheckResult) invokePrivate(
                service,
                "selectLatestEffectiveMergeCheck",
                new Class[]{ProjectActivityLog.class, ProjectActivityLog.class},
                mergeCheckLog,
                resolutionLog
        );

        assertNotNull(selected);
        assertTrue(Boolean.TRUE.equals(selected.getMergeable()));
        assertTrue(selected.getConflicts().isEmpty());
        assertEquals(88L, selected.getMergeRequestId());
        assertEquals(11L, selected.getSourceCommitId());
        assertEquals(22L, selected.getTargetCommitId());
    }

    @Test
    void validateResolutionStrategy_shouldAcceptAllTargetPathOccupiedStrategies() throws Exception {
        invokePrivate(
                service,
                "validateResolutionStrategy",
                new Class[]{ConflictResolutionOption.class, ConflictType.class},
                ConflictResolutionOption.builder()
                        .conflictId("c1")
                        .resolutionStrategy("KEEP_SOURCE")
                        .build(),
                ConflictType.TARGET_PATH_OCCUPIED
        );
        invokePrivate(
                service,
                "validateResolutionStrategy",
                new Class[]{ConflictResolutionOption.class, ConflictType.class},
                ConflictResolutionOption.builder()
                        .conflictId("c1")
                        .resolutionStrategy("KEEP_TARGET")
                        .build(),
                ConflictType.TARGET_PATH_OCCUPIED
        );
        invokePrivate(
                service,
                "validateResolutionStrategy",
                new Class[]{ConflictResolutionOption.class, ConflictType.class},
                ConflictResolutionOption.builder()
                        .conflictId("c1")
                        .resolutionStrategy("SET_TARGET_PATH")
                        .targetPath("docs/target-path.md")
                        .build(),
                ConflictType.TARGET_PATH_OCCUPIED
        );
    }

    @Test
    void buildContentConflictBlocks_shouldSplitIndependentConflictsIntoMultipleBothChangedBlocks() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );

        List<String> blockTypes = blocks.stream().map(ContentConflictBlock::getBlockType).toList();
        assertEquals(List.of("COMMON", "BOTH_CHANGED", "COMMON", "BOTH_CHANGED", "COMMON"), blockTypes);
        assertEquals(2L, blocks.stream().filter(block -> "BOTH_CHANGED".equals(block.getBlockType())).count());
        assertTrue(blocks.stream().allMatch(block -> block.getBlockId() != null && !block.getBlockId().isBlank()));
        assertTrue(blocks.stream().allMatch(block -> block.getDefaultChoice() != null && !block.getDefaultChoice().isBlank()));
    }

    @Test
    void resolveContentConflictResolutionPayload_shouldApplySourceAndTargetChoicePerBlock() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );
        List<ContentConflictBlock> changedBlocks = blocks.stream()
                .filter(block -> "BOTH_CHANGED".equals(block.getBlockType()))
                .toList();
        assertEquals(2, changedBlocks.size());
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(0).getBlockId())
                                .choice("keepSource")
                                .build(),
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(1).getBlockId())
                                .choice("keepTarget")
                                .build()
                ))
                .build();

        Object resolutionPayload = invokePrivate(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        );

        assertEquals("BLOCK_CHOICES", invokePrivate(resolutionPayload, "resolutionMode", new Class[]{}));
        assertEquals("header\nalpha-source\nmid\nbeta-target\ntail",
                invokePrivate(resolutionPayload, "resolvedContent", new Class[]{}));
    }

    @Test
    void createManualContentResolutionCommit_shouldPersistBlockChoiceResultAsSupplementalCommit() throws Exception {
        ProjectActivityLogRepository localActivityLogRepository = mock(ProjectActivityLogRepository.class);
        ProjectCommitRepository localCommitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository localCommitParentRepository = mock(ProjectCommitParentRepository.class);
        ProjectCodeRepositoryRepository localCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
        ProjectBranchRepository localBranchRepository = mock(ProjectBranchRepository.class);
        ProjectMergeRequestRepository localMergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectSnapshotRepository localSnapshotRepository = mock(ProjectSnapshotRepository.class);
        ProjectSnapshotItemRepository localSnapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
        ProjectCommitChangeRepository localCommitChangeRepository = mock(ProjectCommitChangeRepository.class);
        ProjectFileRepository localFileRepository = mock(ProjectFileRepository.class);
        ProjectFileVersionRepository localFileVersionRepository = mock(ProjectFileVersionRepository.class);
        ProjectBlobRepository localBlobRepository = mock(ProjectBlobRepository.class);
        ProjectPermissionService localPermissionService = mock(ProjectPermissionService.class);
        ProjectRepoStorageSupport localStorageSupport = mock(ProjectRepoStorageSupport.class);

        ProjectMergeRequestServiceImpl localService = new ProjectMergeRequestServiceImpl(
                localCodeRepositoryRepository,
                localBranchRepository,
                localMergeRequestRepository,
                localReviewRepository,
                localCheckRunRepository,
                localActivityLogRepository,
                localCommitRepository,
                localCommitParentRepository,
                localSnapshotRepository,
                localSnapshotItemRepository,
                localCommitChangeRepository,
                localFileRepository,
                localFileVersionRepository,
                localBlobRepository,
                localPermissionService,
                localStorageSupport,
                new ObjectMapper()
        );

        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                localService,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha-base\nmid\nbeta-base\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );
        List<ContentConflictBlock> changedBlocks = blocks.stream()
                .filter(block -> "BOTH_CHANGED".equals(block.getBlockType()))
                .toList();
        assertEquals(2, changedBlocks.size());

        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("content-1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(0).getBlockId())
                                .choice("keepSource")
                                .build(),
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(1).getBlockId())
                                .choice("keepTarget")
                                .build()
                ))
                .build();
        Object resolutionPayload = invokePrivate(
                localService,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        );
        String resolvedContent = (String) invokePrivate(resolutionPayload, "resolvedContent", new Class[]{});
        assertEquals("header\nalpha-source\nmid\nbeta-target\ntail", resolvedContent);

        ProjectCodeRepository repo = ProjectCodeRepository.builder()
                .id(100L)
                .projectId(9L)
                .defaultBranchId(12L)
                .build();
        ProjectBranch source = ProjectBranch.builder()
                .id(11L)
                .repositoryId(100L)
                .name("dev")
                .headCommitId(20L)
                .build();
        ProjectBranch target = ProjectBranch.builder()
                .id(12L)
                .repositoryId(100L)
                .name("main")
                .headCommitId(30L)
                .build();
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .sourceHeadCommitId(20L)
                .targetHeadCommitId(30L)
                .build();
        ProjectCommit sourceHead = ProjectCommit.builder()
                .id(20L)
                .repositoryId(100L)
                .branchId(11L)
                .commitNo(2L)
                .snapshotId(200L)
                .build();
        ProjectSnapshotItem currentItem = ProjectSnapshotItem.builder()
                .snapshotId(200L)
                .projectFileId(501L)
                .projectFileVersionId(601L)
                .blobId(701L)
                .canonicalPath("/src/App.java")
                .contentHash("hash-source")
                .build();
        ProjectFile file = ProjectFile.builder()
                .id(501L)
                .projectId(9L)
                .repositoryId(100L)
                .fileName("App.java")
                .canonicalPath("/src/App.java")
                .latestVersionId(601L)
                .build();
        ProjectBlob resolvedBlob = ProjectBlob.builder()
                .id(702L)
                .sha256("hash-resolved")
                .storagePath("upload/project-repo/blobs/hash-resolved.java")
                .sizeBytes(42L)
                .mimeType("text/plain")
                .build();
        ConflictDetail conflict = ConflictDetail.builder()
                .conflictId("content-1")
                .conflictType(ConflictType.CONTENT_CONFLICT)
                .path("/src/App.java")
                .basePath("/src/App.java")
                .sourcePath("/src/App.java")
                .targetPath("/src/App.java")
                .baseCommitId(10L)
                .sourceCommitId(20L)
                .targetCommitId(30L)
                .binaryFile(Boolean.FALSE)
                .build();
        Object context = newMergeCheckContext(mr, repo, source, target);

        when(localBranchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(localCommitRepository.findById(20L)).thenReturn(Optional.of(sourceHead));
        when(localCommitRepository.findTopByRepositoryIdAndBranchIdOrderByCommitNoDesc(100L, 11L))
                .thenReturn(Optional.of(sourceHead));
        when(localCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
            ProjectCommit commit = invocation.getArgument(0);
            if (commit.getId() == null) {
                commit.setId(40L);
            }
            return commit;
        });
        when(localSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.getId() == null) {
                snapshot.setId(400L);
            }
            return snapshot;
        });
        when(localSnapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(200L)).thenReturn(List.of(currentItem));
        when(localStorageSupport.saveTextContent(resolvedContent, "App.java")).thenReturn(resolvedBlob);
        when(localFileRepository.findById(501L)).thenReturn(Optional.of(file));
        when(localFileVersionRepository.countByFileId(501L)).thenReturn(1L);
        when(localFileVersionRepository.save(any(ProjectFileVersion.class))).thenAnswer(invocation -> {
            ProjectFileVersion version = invocation.getArgument(0);
            version.setId(602L);
            return version;
        });
        when(localFileRepository.save(any(ProjectFile.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localSnapshotItemRepository.save(any(ProjectSnapshotItem.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localCommitParentRepository.save(any(ProjectCommitParent.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localCommitChangeRepository.save(any(ProjectCommitChange.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(localMergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Long supplementalCommitId = (Long) invokePrivate(
                localService,
                "createManualContentResolutionCommit",
                new Class[]{context.getClass(), ConflictDetail.class, String.class, Long.class},
                context,
                conflict,
                resolvedContent,
                99L
        );

        assertEquals(40L, supplementalCommitId);
        assertEquals(40L, source.getHeadCommitId());
        assertEquals(40L, mr.getSourceHeadCommitId());
        verify(localStorageSupport).saveTextContent(resolvedContent, "App.java");

        ArgumentCaptor<ProjectCommit> commitCaptor = ArgumentCaptor.forClass(ProjectCommit.class);
        verify(localCommitRepository, org.mockito.Mockito.times(2)).save(commitCaptor.capture());
        ProjectCommit resolutionCommit = commitCaptor.getAllValues().get(0);
        assertEquals(11L, resolutionCommit.getBranchId());
        assertEquals("resolve content conflict: /src/App.java", resolutionCommit.getMessage());
        assertEquals(400L, resolutionCommit.getSnapshotId());

        ArgumentCaptor<ProjectFileVersion> versionCaptor = ArgumentCaptor.forClass(ProjectFileVersion.class);
        verify(localFileVersionRepository).save(versionCaptor.capture());
        ProjectFileVersion savedVersion = versionCaptor.getValue();
        assertEquals(40L, savedVersion.getCommitId());
        assertEquals(702L, savedVersion.getBlobId());
        assertEquals("MODIFY", savedVersion.getChangeType());
        assertEquals(601L, savedVersion.getParentVersionId());

        ArgumentCaptor<ProjectCommitChange> changeCaptor = ArgumentCaptor.forClass(ProjectCommitChange.class);
        verify(localCommitChangeRepository).save(changeCaptor.capture());
        ProjectCommitChange savedChange = changeCaptor.getValue();
        assertEquals(40L, savedChange.getCommitId());
        assertEquals(701L, savedChange.getOldBlobId());
        assertEquals(702L, savedChange.getNewBlobId());
        assertTrue(savedChange.getDiffSummaryJson().contains("\"contentConflictResolve\":true"));

        ArgumentCaptor<ProjectSnapshotItem> snapshotItemCaptor = ArgumentCaptor.forClass(ProjectSnapshotItem.class);
        verify(localSnapshotItemRepository).save(snapshotItemCaptor.capture());
        ProjectSnapshotItem savedSnapshotItem = snapshotItemCaptor.getValue();
        assertEquals(400L, savedSnapshotItem.getSnapshotId());
        assertEquals(602L, savedSnapshotItem.getProjectFileVersionId());
        assertEquals(702L, savedSnapshotItem.getBlobId());
        assertEquals("hash-resolved", savedSnapshotItem.getContentHash());
    }

    @Test
    void resolveContentConflictResolutionPayload_afterRebuild_shouldNotKeepAlreadyChosenBlockAsConflict() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> initialBlocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );
        List<ContentConflictBlock> changedBlocks = initialBlocks.stream()
                .filter(block -> "BOTH_CHANGED".equals(block.getBlockType()))
                .toList();
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(0).getBlockId())
                                .choice("keepSource")
                                .build(),
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(1).getBlockId())
                                .choice("keepTarget")
                                .build()
                ))
                .build();

        Object resolutionPayload = invokePrivate(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                initialBlocks
        );
        String resolvedContent = (String) invokePrivate(resolutionPayload, "resolvedContent", new Class[]{});

        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> rebuiltBlocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                resolvedContent,
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );

        assertEquals(1L, rebuiltBlocks.stream().filter(block -> "BOTH_CHANGED".equals(block.getBlockType())).count());
    }

    @Test
    void buildContentConflictBlocks_shouldCollapseResolvedChangedLinesToCommon() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nbeta\ntail",
                "header\nalpha-source\nbeta-target\ntail",
                "header\nalpha-target\nbeta-target\ntail"
        );

        List<String> blockTypes = blocks.stream().map(ContentConflictBlock::getBlockType).toList();
        assertEquals(List.of("COMMON", "BOTH_CHANGED", "COMMON"), blockTypes);
        assertEquals(List.of("tail"), blocks.get(2).getSourceLines());
        assertEquals(List.of("tail"), blocks.get(2).getTargetLines());
    }

    @Test
    void resolveContentConflictResolutionPayload_shouldSupportBothSideMergeOrder() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );
        List<ContentConflictBlock> changedBlocks = blocks.stream()
                .filter(block -> "BOTH_CHANGED".equals(block.getBlockType()))
                .toList();
        assertEquals(2, changedBlocks.size());
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(0).getBlockId())
                                .choice("keepBothSourceThenTarget")
                                .build(),
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(1).getBlockId())
                                .choice("keepBothTargetThenSource")
                                .build()
                ))
                .build();

        Object resolutionPayload = invokePrivate(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        );

        assertEquals("BLOCK_CHOICES", invokePrivate(resolutionPayload, "resolutionMode", new Class[]{}));
        assertEquals("header\nalpha-source\nalpha-target\nmid\nbeta-target\nbeta-source\ntail",
                invokePrivate(resolutionPayload, "resolvedContent", new Class[]{}));
    }

    @Test
    void resolveContentConflictResolutionPayload_shouldSupportManualChoice() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "header\nalpha\nmid\nbeta\ntail",
                "header\nalpha-source\nmid\nbeta-source\ntail",
                "header\nalpha-target\nmid\nbeta-target\ntail"
        );
        List<ContentConflictBlock> changedBlocks = blocks.stream()
                .filter(block -> "BOTH_CHANGED".equals(block.getBlockType()))
                .toList();
        assertEquals(2, changedBlocks.size());
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(0).getBlockId())
                                .choice("manual")
                                .resolvedContent("alpha-manual-1\nalpha-manual-2")
                                .build(),
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId(changedBlocks.get(1).getBlockId())
                                .choice("keepTarget")
                                .build()
                ))
                .build();

        Object resolutionPayload = invokePrivate(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        );

        assertEquals("BLOCK_CHOICES", invokePrivate(resolutionPayload, "resolutionMode", new Class[]{}));
        assertEquals("header\nalpha-manual-1\nalpha-manual-2\nmid\nbeta-target\ntail",
                invokePrivate(resolutionPayload, "resolvedContent", new Class[]{}));
    }

    @Test
    void resolveContentConflictResolutionPayload_shouldRejectUnknownBlockId() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "line-1\nbase-only\nline-3",
                "line-1\nsource-only\nline-3",
                "line-1\ntarget-only\nline-3"
        );
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .blockChoices(List.of(
                        ContentConflictResolveRequest.BlockChoice.builder()
                                .blockId("unknown-block-id")
                                .choice("keepSource")
                                .build()
                ))
                .build();

        BusinessExceptionWrapper thrown = assertThrows(BusinessExceptionWrapper.class, () -> invokePrivateWithBusinessException(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        ));

        assertTrue(thrown.getMessage().contains("blockId"));
    }

    @Test
    void resolveContentConflictResolutionPayload_shouldKeepLegacyResolvedContentCompatibility() throws Exception {
        @SuppressWarnings("unchecked")
        List<ContentConflictBlock> blocks = (List<ContentConflictBlock>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "line-1\nbase-only\nline-3",
                "line-1\nsource-only\nline-3",
                "line-1\ntarget-only\nline-3"
        );
        ContentConflictResolveRequest request = ContentConflictResolveRequest.builder()
                .conflictId("c1")
                .resolvedContent("legacy-content")
                .build();

        Object resolutionPayload = invokePrivate(
                service,
                "resolveContentConflictResolutionPayload",
                new Class[]{ContentConflictResolveRequest.class, List.class},
                request,
                blocks
        );

        assertEquals("RAW_CONTENT", invokePrivate(resolutionPayload, "resolutionMode", new Class[]{}));
        assertEquals("legacy-content", invokePrivate(resolutionPayload, "resolvedContent", new Class[]{}));
    }

    @Test
    void requireContentConflict_shouldRejectNonContentConflict() {
        ConflictDetail conflict = ConflictDetail.builder()
                .conflictId("c1")
                .conflictType(ConflictType.DELETE_MODIFY_CONFLICT)
                .build();

        BusinessExceptionWrapper thrown = assertThrows(BusinessExceptionWrapper.class, () -> invokePrivateWithBusinessException(
                service,
                "requireContentConflict",
                new Class[]{String.class, Map.class},
                "c1",
                Map.of("c1", conflict)
        ));

        assertTrue(thrown.getMessage().contains("content editor"));
    }

    @Test
    void buildContentConflictMetadata_shouldExposeEditorHints() throws Exception {
        ConflictDetail conflict = ConflictDetail.builder()
                .conflictId("c1")
                .conflictType(ConflictType.CONTENT_CONFLICT)
                .path("/src/App.java")
                .fileName("App.java")
                .basePath("/src/App.java")
                .sourcePath("/src/App.java")
                .targetPath("/src/App.java")
                .binaryFile(Boolean.FALSE)
                .baseContentHash("base")
                .sourceContentHash("source")
                .targetContentHash("target")
                .build();
        MergeCheckResult latest = MergeCheckResult.builder()
                .mergeable(Boolean.FALSE)
                .requiresRecheck(Boolean.FALSE)
                .requiresBranchUpdate(Boolean.FALSE)
                .blockingReasons(new java.util.ArrayList<>(List.of("UNRESOLVED_CONFLICTS")))
                .build();
        Object snapshotView = newConflictSnapshotView(
                snapshotItem(1L, "/src/App.java"),
                snapshotItem(2L, "/src/App.java"),
                snapshotItem(3L, "/src/App.java"),
                Boolean.FALSE
        );

        @SuppressWarnings("unchecked")
        Map<String, Object> metadata = (Map<String, Object>) invokePrivate(
                service,
                "buildContentConflictMetadata",
                new Class[]{ConflictDetail.class, MergeCheckResult.class, snapshotView.getClass()},
                conflict,
                latest,
                snapshotView
        );

        assertEquals("java", metadata.get("language"));
        assertEquals("java", metadata.get("fileExtension"));
        assertEquals(Boolean.TRUE, metadata.get("onlineEditable"));
        assertEquals(Boolean.TRUE, metadata.get("allowOnlineEdit"));
        assertEquals(Boolean.FALSE, metadata.get("readOnly"));
        assertEquals(Boolean.FALSE, metadata.get("binaryFile"));
        assertEquals(Boolean.FALSE, metadata.get("hasPathChange"));
        assertEquals(Boolean.FALSE, metadata.get("requiresRecheck"));
        assertEquals(Boolean.FALSE, metadata.get("requiresBranchUpdate"));
    }

    @Test
    void applyPreMergeGates_shouldIgnoreSystemInternalFailedChecks() throws Exception {
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectMergeRequestServiceImpl localService = newServiceForGateTests(localReviewRepository, localCheckRunRepository);

        ProjectMergeRequest mr = ProjectMergeRequest.builder().id(88L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).headCommitId(120L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).protectedFlag(true).build();
        MergeCheckResult baseResult = MergeCheckResult.builder()
                .mergeable(Boolean.TRUE)
                .conflicts(new java.util.ArrayList<>())
                .blockingReasons(new java.util.ArrayList<>())
                .build();

        when(localReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(88L))
                .thenReturn(List.of(ProjectReview.builder().reviewResult("approve").build()));
        when(localCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(88L))
                .thenReturn(List.of(checkRun(1L, 88L, 120L, "merge_conflict_resolution", "failed", "resolution failed")));

        MergeCheckResult updated = (MergeCheckResult) invokePrivate(
                localService,
                "applyPreMergeGates",
                new Class[]{MergeCheckResult.class, ProjectMergeRequest.class, ProjectBranch.class, ProjectBranch.class},
                baseResult,
                mr,
                source,
                target
        );

        assertFalse(updated.getBlockingReasons().contains("FAILED_CHECK_RUN"));
        assertEquals(1, updated.getEffectiveChecks().size());
        assertEquals(Boolean.TRUE, updated.getEffectiveChecks().get(0).getSystemInternal());
        assertEquals(Boolean.FALSE, updated.getEffectiveChecks().get(0).getBlockingMerge());
    }

    @Test
    void applyPreMergeGates_shouldNotBlockWhenLatestCheckOfSameTypeIsSuccessful() throws Exception {
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectMergeRequestServiceImpl localService = newServiceForGateTests(localReviewRepository, localCheckRunRepository);

        ProjectMergeRequest mr = ProjectMergeRequest.builder().id(89L).build();
        ProjectBranch source = ProjectBranch.builder().id(21L).headCommitId(220L).build();
        ProjectBranch target = ProjectBranch.builder().id(22L).protectedFlag(true).build();
        MergeCheckResult baseResult = MergeCheckResult.builder()
                .mergeable(Boolean.TRUE)
                .conflicts(new java.util.ArrayList<>())
                .blockingReasons(new java.util.ArrayList<>())
                .build();

        when(localReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(89L))
                .thenReturn(List.of(ProjectReview.builder().reviewResult("approve").build()));
        when(localCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(89L))
                .thenReturn(List.of(
                        checkRun(2L, 89L, 220L, "ci", "success", "ci recovered"),
                        checkRun(1L, 89L, 220L, "ci", "failed", "ci failed previously")
                ));

        MergeCheckResult updated = (MergeCheckResult) invokePrivate(
                localService,
                "applyPreMergeGates",
                new Class[]{MergeCheckResult.class, ProjectMergeRequest.class, ProjectBranch.class, ProjectBranch.class},
                baseResult,
                mr,
                source,
                target
        );

        assertFalse(updated.getBlockingReasons().contains("FAILED_CHECK_RUN"));
        assertTrue(updated.getBlockingChecks().isEmpty());
        assertEquals("success", updated.getEffectiveChecks().get(0).getCheckStatus());
    }

    @Test
    void applyPreMergeGates_shouldBlockProtectedBranchWithFailedActionableChecks() throws Exception {
        ProjectReviewRepository localReviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository localCheckRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectMergeRequestServiceImpl localService = newServiceForGateTests(localReviewRepository, localCheckRunRepository);

        ProjectMergeRequest mr = ProjectMergeRequest.builder().id(90L).build();
        ProjectBranch source = ProjectBranch.builder().id(31L).headCommitId(320L).build();
        ProjectBranch target = ProjectBranch.builder().id(32L).protectedFlag(true).build();
        MergeCheckResult baseResult = MergeCheckResult.builder()
                .mergeable(Boolean.TRUE)
                .conflicts(new java.util.ArrayList<>())
                .blockingReasons(new java.util.ArrayList<>())
                .build();

        when(localReviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(90L))
                .thenReturn(List.of(ProjectReview.builder().reviewResult("approve").build()));
        when(localCheckRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(90L))
                .thenReturn(List.of(
                        checkRun(3L, 90L, 320L, "ci", "failed", "unit test failed"),
                        checkRun(2L, 90L, 320L, "merge_conflict_resolution", "failed", "resolution failed")
                ));

        MergeCheckResult updated = (MergeCheckResult) invokePrivate(
                localService,
                "applyPreMergeGates",
                new Class[]{MergeCheckResult.class, ProjectMergeRequest.class, ProjectBranch.class, ProjectBranch.class},
                baseResult,
                mr,
                source,
                target
        );

        assertTrue(updated.getBlockingReasons().contains("FAILED_CHECK_RUN"));
        assertEquals(1, updated.getBlockingChecks().size());
        assertEquals("ci", updated.getBlockingChecks().get(0).getCheckType());
        assertTrue(updated.getSummary().toLowerCase().contains("ci"));
        assertTrue(updated.getSuggestedAction().contains("same checkType"));
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
    }

    private ProjectMergeRequestServiceImpl newServiceForGateTests(ProjectReviewRepository reviewRepository,
                                                                  ProjectCheckRunRepository checkRunRepository) {
        return new ProjectMergeRequestServiceImpl(
                mock(ProjectCodeRepositoryRepository.class),
                mock(ProjectBranchRepository.class),
                mock(ProjectMergeRequestRepository.class),
                reviewRepository,
                checkRunRepository,
                mock(ProjectActivityLogRepository.class),
                mock(ProjectCommitRepository.class),
                mock(ProjectCommitParentRepository.class),
                mock(ProjectSnapshotRepository.class),
                mock(ProjectSnapshotItemRepository.class),
                mock(ProjectCommitChangeRepository.class),
                mock(ProjectFileRepository.class),
                mock(ProjectFileVersionRepository.class),
                mock(ProjectBlobRepository.class),
                mock(ProjectPermissionService.class),
                mock(ProjectRepoStorageSupport.class),
                new ObjectMapper()
        );
    }

    private Object newMergeCheckContext(ProjectMergeRequest mergeRequest,
                                        ProjectCodeRepository repo,
                                        ProjectBranch source,
                                        ProjectBranch target) throws Exception {
        Class<?> contextClass = Class.forName("com.alikeyou.itmoduleproject.service.impl.ProjectMergeRequestServiceImpl$MergeCheckContext");
        java.lang.reflect.Constructor<?> constructor = contextClass.getDeclaredConstructor(
                ProjectMergeRequest.class,
                ProjectCodeRepository.class,
                ProjectBranch.class,
                ProjectBranch.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(mergeRequest, repo, source, target);
    }

    private Object newMergeExecutionState(ProjectCommit sourceHead,
                                          ProjectCommit targetHead,
                                          ProjectCommit mergeBase,
                                          Map<String, ProjectSnapshotItem> baseSnapshot,
                                          Map<String, ProjectSnapshotItem> sourceSnapshot,
                                          Map<String, ProjectSnapshotItem> targetSnapshot) throws Exception {
        Class<?> executionStateClass = Class.forName("com.alikeyou.itmoduleproject.service.impl.ProjectMergeRequestServiceImpl$MergeExecutionState");
        java.lang.reflect.Constructor<?> constructor = executionStateClass.getDeclaredConstructor(
                ProjectCommit.class,
                ProjectCommit.class,
                ProjectCommit.class,
                Map.class,
                Map.class,
                Map.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(sourceHead, targetHead, mergeBase, baseSnapshot, sourceSnapshot, targetSnapshot);
    }

    private Object invokePrivateWithBusinessException(Object target, String methodName, Class<?>[] parameterTypes, Object... args) {
        try {
            invokePrivate(target, methodName, parameterTypes, args);
            return null;
        } catch (Exception ex) {
            Throwable cause = ex.getCause();
            throw new BusinessExceptionWrapper(cause == null ? ex.getMessage() : cause.getMessage());
        }
    }

    private ProjectCommit commit(Long id) {
        return ProjectCommit.builder()
                .id(id)
                .repositoryId(10L)
                .branchId(20L)
                .commitNo(id)
                .displaySha("sha" + id)
                .message("commit-" + id)
                .commitType("normal")
                .snapshotId(id + 100)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ProjectCommitParent parent(Long commitId, Long parentCommitId) {
        return ProjectCommitParent.builder()
                .commitId(commitId)
                .parentCommitId(parentCommitId)
                .parentOrder(1)
                .build();
    }

    private ProjectSnapshotItem snapshotItem(Long blobId, String path) {
        return ProjectSnapshotItem.builder()
                .blobId(blobId)
                .canonicalPath(path)
                .contentHash("hash-" + blobId)
                .projectFileId(blobId)
                .projectFileVersionId(blobId)
                .build();
    }

    private ProjectCheckRun checkRun(Long id,
                                     Long mergeRequestId,
                                     Long commitId,
                                     String checkType,
                                     String checkStatus,
                                     String summary) {
        return ProjectCheckRun.builder()
                .id(id)
                .repositoryId(1L)
                .mergeRequestId(mergeRequestId)
                .commitId(commitId)
                .checkType(checkType)
                .checkStatus(checkStatus)
                .summary(summary)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private Object newConflictSnapshotView(ProjectSnapshotItem baseItem,
                                           ProjectSnapshotItem sourceItem,
                                           ProjectSnapshotItem targetItem,
                                           Boolean binaryFile) throws Exception {
        Class<?> snapshotViewClass = Class.forName("com.alikeyou.itmoduleproject.service.impl.ProjectMergeRequestServiceImpl$ConflictSnapshotView");
        java.lang.reflect.Constructor<?> constructor = snapshotViewClass.getDeclaredConstructor(
                ProjectSnapshotItem.class,
                ProjectSnapshotItem.class,
                ProjectSnapshotItem.class,
                String.class,
                String.class,
                String.class,
                List.class,
                List.class,
                List.class,
                Boolean.class
        );
        constructor.setAccessible(true);
        return constructor.newInstance(
                baseItem,
                sourceItem,
                targetItem,
                "base",
                "source",
                "target",
                List.of("base"),
                List.of("source"),
                List.of("target"),
                binaryFile
        );
    }

    private static class BusinessExceptionWrapper extends RuntimeException {
        private BusinessExceptionWrapper(String message) {
            super(message);
        }
    }
}
