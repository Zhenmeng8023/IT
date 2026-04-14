package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.support.diff.ConflictDetail;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.ConflictType;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;
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

        assertTrue(Boolean.TRUE.equals(requiresRecheck));
        assertFalse(Boolean.TRUE.equals(mergeable));
        assertTrue(blockingReasons.contains("RECHECK_REQUIRED"));
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
    void buildContentConflictBlocks_shouldPreserveCommonPrefixAndSuffix() throws Exception {
        @SuppressWarnings("unchecked")
        List<Object> blocks = (List<Object>) invokePrivate(
                service,
                "buildContentConflictBlocks",
                new Class[]{String.class, String.class, String.class},
                "line-1\nbase-only\nline-3",
                "line-1\nsource-only\nline-3",
                "line-1\ntarget-only\nline-3"
        );

        assertEquals(3, blocks.size());
        assertEquals("COMMON", invokePrivate(blocks.get(0), "getBlockType", new Class[]{}));
        assertEquals("CONFLICT", invokePrivate(blocks.get(1), "getBlockType", new Class[]{}));
        assertEquals("COMMON", invokePrivate(blocks.get(2), "getBlockType", new Class[]{}));

        @SuppressWarnings("unchecked")
        List<String> middleBaseLines = (List<String>) invokePrivate(blocks.get(1), "getBaseLines", new Class[]{});
        @SuppressWarnings("unchecked")
        List<String> middleSourceLines = (List<String>) invokePrivate(blocks.get(1), "getSourceLines", new Class[]{});
        @SuppressWarnings("unchecked")
        List<String> middleTargetLines = (List<String>) invokePrivate(blocks.get(1), "getTargetLines", new Class[]{});
        assertEquals(List.of("base-only"), middleBaseLines);
        assertEquals(List.of("source-only"), middleSourceLines);
        assertEquals(List.of("target-only"), middleTargetLines);
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

    private Object invokePrivate(Object target, String methodName, Class<?>[] parameterTypes, Object... args) throws Exception {
        Method method = target.getClass().getDeclaredMethod(methodName, parameterTypes);
        method.setAccessible(true);
        return method.invoke(target, args);
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

    private static class BusinessExceptionWrapper extends RuntimeException {
        private BusinessExceptionWrapper(String message) {
            super(message);
        }
    }
}
