package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectMergeRequest;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mockito.ArgumentCaptor;

class ProjectMergeRequestServiceImplTest {

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

        assertTrue(Boolean.TRUE.equals(requiresRecheck));
        assertFalse(Boolean.TRUE.equals(mergeable));
        assertTrue(blockingReasons.contains("RECHECK_REQUIRED"));
    }

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
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
}
