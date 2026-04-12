package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshotItem;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProjectMergeRequestServiceImplTest {

    private final ProjectCommitRepository commitRepository = mock(ProjectCommitRepository.class);
    private final ProjectCommitParentRepository commitParentRepository = mock(ProjectCommitParentRepository.class);
    private final ProjectMergeRequestServiceImpl service = new ProjectMergeRequestServiceImpl(
            mock(ProjectCodeRepositoryRepository.class),
            mock(ProjectBranchRepository.class),
            mock(ProjectMergeRequestRepository.class),
            mock(ProjectReviewRepository.class),
            mock(ProjectCheckRunRepository.class),
            commitRepository,
            commitParentRepository,
            mock(ProjectSnapshotRepository.class),
            mock(ProjectSnapshotItemRepository.class),
            mock(ProjectCommitChangeRepository.class),
            mock(ProjectFileRepository.class),
            mock(ProjectFileVersionRepository.class),
            mock(ProjectBlobRepository.class),
            mock(ProjectPermissionService.class)
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
