package com.alikeyou.itmoduleproject.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.alikeyou.itmoduleproject.dto.ConflictResolutionRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMergeRequestCreateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectActivityLog;
import com.alikeyou.itmoduleproject.entity.ProjectBlob;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCheckRun;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectCommitParent;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
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
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.diff.ConflictResolutionOption;
import com.alikeyou.itmoduleproject.support.diff.ConflictType;
import com.alikeyou.itmoduleproject.support.diff.MergeCheckResult;
import com.alikeyou.itmoduleproject.vo.ProjectMergeRequestVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectMergeRequestServiceImplFlowTest {

    @Test
    void create_shouldCreateMergeRequest() {
        var fixture = newFixture();
        ProjectCodeRepository repo = ProjectCodeRepository.builder().id(100L).projectId(200L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).repositoryId(100L).name("feature/a").headCommitId(501L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).repositoryId(100L).name("main").headCommitId(601L).build();

        when(fixture.repoRepository.findByProjectId(200L)).thenReturn(Optional.of(repo));
        when(fixture.branchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(fixture.branchRepository.findById(12L)).thenReturn(Optional.of(target));
        when(fixture.mergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> {
            ProjectMergeRequest saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(88L);
            }
            return saved;
        });
        when(fixture.reviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(88L)).thenReturn(List.of());
        when(fixture.checkRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(88L)).thenReturn(List.of());

        ProjectMergeRequestCreateRequest request = new ProjectMergeRequestCreateRequest();
        request.setProjectId(200L);
        request.setSourceBranchId(11L);
        request.setTargetBranchId(12L);
        request.setTitle("");
        request.setDescription("flow-test");

        ProjectMergeRequestVO created = fixture.service.create(request, 9L);

        assertEquals(88L, created.getId());
        assertEquals("open", created.getStatus());
        assertEquals(11L, created.getSourceBranchId());
        assertEquals(12L, created.getTargetBranchId());
        ArgumentCaptor<ProjectActivityLog> logCaptor = ArgumentCaptor.forClass(ProjectActivityLog.class);
        verify(fixture.activityLogRepository).save(logCaptor.capture());
        assertEquals("mr_create", logCaptor.getValue().getAction());
    }

    @Test
    void checkMerge_shouldPassWhenNoConflict() {
        var fixture = newFixture();
        bootstrapMergeContext(fixture, false);

        MergeCheckResult result = fixture.service.checkMerge(88L, 1L);

        assertTrue(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts() == null || result.getConflicts().isEmpty());
    }

    @Test
    void checkMerge_shouldDetectConflict() {
        var fixture = newFixture();
        bootstrapMergeContext(fixture, true);

        MergeCheckResult result = fixture.service.checkMerge(88L, 1L);

        assertFalse(Boolean.TRUE.equals(result.getMergeable()));
        assertTrue(result.getConflicts().stream()
                .anyMatch(conflict -> ConflictType.DELETE_MODIFY_CONFLICT.equals(conflict.getConflictType())));
    }

    @Test
    void resolveStructuredConflicts_shouldApplyDeleteModifyResolution() {
        var fixture = newFixture();
        bootstrapMergeContext(fixture, true);
        MergeCheckResult latest = fixture.service.checkMerge(88L, 1L);
        String conflictId = latest.getConflicts().stream()
                .filter(conflict -> ConflictType.DELETE_MODIFY_CONFLICT.equals(conflict.getConflictType()))
                .findFirst()
                .map(conflict -> conflict.getConflictId())
                .orElseThrow();

        ConflictResolutionRequest request = ConflictResolutionRequest.builder()
                .options(List.of(ConflictResolutionOption.builder()
                        .conflictId(conflictId)
                        .resolutionStrategy("KEEP_SOURCE")
                        .build()))
                .build();

        var resolved = fixture.service.resolveStructuredConflicts(88L, request, 1L);

        assertNotNull(resolved.getLatestMergeCheck());
        assertTrue(resolved.getMetadata().containsKey("resolvedConflictIds"));
    }

    @Test
    void merge_shouldFailWhenProtectedBranchMissingReview() {
        var fixture = newFixture();

        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .status("open")
                .build();
        ProjectCodeRepository repo = ProjectCodeRepository.builder().id(100L).projectId(200L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).repositoryId(100L).name("feature/a").headCommitId(2L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).repositoryId(100L).name("main").headCommitId(2L).protectedFlag(true).build();
        ProjectCommit head = ProjectCommit.builder().id(2L).repositoryId(100L).branchId(11L).snapshotId(120L).build();

        when(fixture.mergeRequestRepository.findById(88L)).thenReturn(Optional.of(mr));
        when(fixture.repoRepository.findById(100L)).thenReturn(Optional.of(repo));
        when(fixture.branchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(fixture.branchRepository.findById(12L)).thenReturn(Optional.of(target));
        when(fixture.commitRepository.findById(2L)).thenReturn(Optional.of(head));
        when(fixture.reviewRepository.findByMergeRequestIdOrderByCreatedAtAsc(88L)).thenReturn(List.of());
        when(fixture.checkRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(88L)).thenReturn(List.of());

        assertThrows(BusinessException.class, () -> fixture.service.merge(88L, 1L));
    }

    private void bootstrapMergeContext(Fixture fixture, boolean withConflict) {
        ProjectMergeRequest mr = ProjectMergeRequest.builder()
                .id(88L)
                .repositoryId(100L)
                .sourceBranchId(11L)
                .targetBranchId(12L)
                .status("open")
                .build();
        ProjectCodeRepository repo = ProjectCodeRepository.builder().id(100L).projectId(200L).build();
        ProjectBranch source = ProjectBranch.builder().id(11L).repositoryId(100L).name("feature/a").headCommitId(withConflict ? 2L : 2L).build();
        ProjectBranch target = ProjectBranch.builder().id(12L).repositoryId(100L).name("main").headCommitId(withConflict ? 3L : 2L).protectedFlag(false).build();

        ProjectCommit base = ProjectCommit.builder().id(1L).repositoryId(100L).branchId(12L).snapshotId(100L).build();
        ProjectCommit sourceHead = ProjectCommit.builder().id(2L).repositoryId(100L).branchId(11L).snapshotId(withConflict ? 200L : 100L).build();
        ProjectCommit targetHead = ProjectCommit.builder().id(withConflict ? 3L : 2L).repositoryId(100L).branchId(12L).snapshotId(withConflict ? 300L : 100L).build();

        when(fixture.mergeRequestRepository.findById(88L)).thenReturn(Optional.of(mr));
        when(fixture.repoRepository.findById(100L)).thenReturn(Optional.of(repo));
        when(fixture.branchRepository.findById(11L)).thenReturn(Optional.of(source));
        when(fixture.branchRepository.findById(12L)).thenReturn(Optional.of(target));
        when(fixture.commitRepository.findById(1L)).thenReturn(Optional.of(base));
        when(fixture.commitRepository.findById(2L)).thenReturn(Optional.of(sourceHead));
        when(fixture.commitRepository.findById(3L)).thenReturn(Optional.of(targetHead));
        when(fixture.parentRepository.findByCommitIdOrderByParentOrderAsc(1L)).thenReturn(List.of());
        when(fixture.parentRepository.findByCommitIdOrderByParentOrderAsc(2L))
                .thenReturn(withConflict ? List.of(parent(2L, 1L)) : List.of());
        when(fixture.parentRepository.findByCommitIdOrderByParentOrderAsc(3L))
                .thenReturn(withConflict ? List.of(parent(3L, 1L)) : List.of());

        if (withConflict) {
            when(fixture.snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(100L))
                    .thenReturn(List.of(snapshotItem(1L, "src/App.java")));
            when(fixture.snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(200L))
                    .thenReturn(List.of());
            when(fixture.snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(300L))
                    .thenReturn(List.of(snapshotItem(3L, "src/App.java")));
        } else {
            when(fixture.snapshotItemRepository.findBySnapshotIdOrderByCanonicalPathAsc(100L))
                    .thenReturn(List.of(snapshotItem(1L, "src/App.java")));
        }

        when(fixture.blobRepository.findById(anyLong())).thenAnswer(invocation -> Optional.of(ProjectBlob.builder()
                .id(invocation.getArgument(0))
                .mimeType("text/plain")
                .build()));
        when(fixture.checkRunRepository.findByMergeRequestIdOrderByCreatedAtDesc(88L)).thenReturn(List.of());
        when(fixture.activityLogRepository.findTopByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(eq(88L), any()))
                .thenReturn(Optional.empty());
        when(fixture.activityLogRepository.findByMergeRequestIdAndActionOrderByCreatedAtDescIdDesc(eq(88L), any()))
                .thenReturn(List.of());
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

    private Fixture newFixture() {
        ProjectCodeRepositoryRepository repoRepository = mock(ProjectCodeRepositoryRepository.class);
        ProjectBranchRepository branchRepository = mock(ProjectBranchRepository.class);
        ProjectMergeRequestRepository mergeRequestRepository = mock(ProjectMergeRequestRepository.class);
        ProjectReviewRepository reviewRepository = mock(ProjectReviewRepository.class);
        ProjectCheckRunRepository checkRunRepository = mock(ProjectCheckRunRepository.class);
        ProjectActivityLogRepository activityLogRepository = mock(ProjectActivityLogRepository.class);
        ProjectCommitRepository commitRepository = mock(ProjectCommitRepository.class);
        ProjectCommitParentRepository parentRepository = mock(ProjectCommitParentRepository.class);
        ProjectSnapshotRepository snapshotRepository = mock(ProjectSnapshotRepository.class);
        ProjectSnapshotItemRepository snapshotItemRepository = mock(ProjectSnapshotItemRepository.class);
        ProjectCommitChangeRepository commitChangeRepository = mock(ProjectCommitChangeRepository.class);
        ProjectFileRepository fileRepository = mock(ProjectFileRepository.class);
        ProjectFileVersionRepository fileVersionRepository = mock(ProjectFileVersionRepository.class);
        ProjectBlobRepository blobRepository = mock(ProjectBlobRepository.class);
        ProjectPermissionService permissionService = mock(ProjectPermissionService.class);
        ProjectRepoStorageSupport repoStorageSupport = mock(ProjectRepoStorageSupport.class);

        AtomicLong checkRunId = new AtomicLong(10L);
        AtomicLong activityId = new AtomicLong(100L);
        when(mergeRequestRepository.save(any(ProjectMergeRequest.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(checkRunRepository.save(any(ProjectCheckRun.class))).thenAnswer(invocation -> {
            ProjectCheckRun saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(checkRunId.incrementAndGet());
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });
        when(activityLogRepository.save(any(ProjectActivityLog.class))).thenAnswer(invocation -> {
            ProjectActivityLog saved = invocation.getArgument(0);
            if (saved.getId() == null) {
                saved.setId(activityId.incrementAndGet());
            }
            if (saved.getCreatedAt() == null) {
                saved.setCreatedAt(LocalDateTime.now());
            }
            return saved;
        });

        ProjectMergeRequestServiceImpl service = new ProjectMergeRequestServiceImpl(
                repoRepository,
                branchRepository,
                mergeRequestRepository,
                reviewRepository,
                checkRunRepository,
                activityLogRepository,
                commitRepository,
                parentRepository,
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
        return new Fixture(
                service,
                repoRepository,
                branchRepository,
                mergeRequestRepository,
                reviewRepository,
                checkRunRepository,
                activityLogRepository,
                commitRepository,
                parentRepository,
                snapshotItemRepository,
                blobRepository
        );
    }

    private record Fixture(ProjectMergeRequestServiceImpl service,
                           ProjectCodeRepositoryRepository repoRepository,
                           ProjectBranchRepository branchRepository,
                           ProjectMergeRequestRepository mergeRequestRepository,
                           ProjectReviewRepository reviewRepository,
                           ProjectCheckRunRepository checkRunRepository,
                           ProjectActivityLogRepository activityLogRepository,
                           ProjectCommitRepository commitRepository,
                           ProjectCommitParentRepository parentRepository,
                           ProjectSnapshotItemRepository snapshotItemRepository,
                           ProjectBlobRepository blobRepository) {
    }
}
