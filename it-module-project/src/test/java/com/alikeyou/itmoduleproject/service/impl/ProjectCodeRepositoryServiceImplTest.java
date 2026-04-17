package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectBranch;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.entity.ProjectSnapshot;
import com.alikeyou.itmoduleproject.repository.ProjectBranchRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectCodeRepositoryServiceImplTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectBranchRepository projectBranchRepository = mock(ProjectBranchRepository.class);
    private final ProjectCommitRepository projectCommitRepository = mock(ProjectCommitRepository.class);
    private final ProjectSnapshotRepository projectSnapshotRepository = mock(ProjectSnapshotRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport = mock(ProjectRepositoryBootstrapSupport.class);

    private final ProjectCodeRepositoryServiceImpl service = new ProjectCodeRepositoryServiceImpl(
            projectRepository,
            projectCodeRepositoryRepository,
            projectBranchRepository,
            projectCommitRepository,
            projectSnapshotRepository,
            projectPermissionService,
            projectRepositoryBootstrapSupport
    );

    @Test
    void initRepository_shouldCreateMainAndDevOnSameEmptyBootstrapCommit() {
        long projectId = 101L;
        long userId = 99L;
        Project project = Project.builder()
                .id(projectId)
                .name("demo")
                .authorId(userId)
                .build();

        when(projectRepository.findById(projectId)).thenReturn(Optional.of(project));
        when(projectCodeRepositoryRepository.findByProjectId(projectId)).thenReturn(Optional.empty());
        when(projectCodeRepositoryRepository.save(any(ProjectCodeRepository.class))).thenAnswer(invocation -> {
            ProjectCodeRepository repository = invocation.getArgument(0);
            if (repository.getId() == null) {
                repository.setId(201L);
            }
            return repository;
        });

        AtomicLong branchIdGenerator = new AtomicLong(301L);
        when(projectBranchRepository.save(any(ProjectBranch.class))).thenAnswer(invocation -> {
            ProjectBranch branch = invocation.getArgument(0);
            if (branch.getId() == null) {
                branch.setId(branchIdGenerator.getAndIncrement());
            }
            return branch;
        });

        when(projectSnapshotRepository.save(any(ProjectSnapshot.class))).thenAnswer(invocation -> {
            ProjectSnapshot snapshot = invocation.getArgument(0);
            if (snapshot.getId() == null) {
                snapshot.setId(401L);
            }
            return snapshot;
        });

        when(projectCommitRepository.save(any(ProjectCommit.class))).thenAnswer(invocation -> {
            ProjectCommit commit = invocation.getArgument(0);
            if (commit.getId() == null) {
                commit.setId(501L);
            }
            return commit;
        });

        ProjectCodeRepositoryVO result = service.initRepository(projectId, userId);

        assertNotNull(result);
        assertNotNull(result.getDefaultBranchId());
        assertNotNull(result.getHeadCommitId());
        assertEquals(501L, result.getHeadCommitId());

        ArgumentCaptor<ProjectCommit> commitCaptor = ArgumentCaptor.forClass(ProjectCommit.class);
        verify(projectCommitRepository).save(commitCaptor.capture());
        ProjectCommit bootstrapCommit = commitCaptor.getValue();
        assertEquals("bootstrap", bootstrapCommit.getCommitType());
        assertEquals(401L, bootstrapCommit.getSnapshotId());

        ArgumentCaptor<ProjectSnapshot> snapshotCaptor = ArgumentCaptor.forClass(ProjectSnapshot.class);
        verify(projectSnapshotRepository, org.mockito.Mockito.times(2)).save(snapshotCaptor.capture());
        List<ProjectSnapshot> snapshotSaves = snapshotCaptor.getAllValues();
        assertEquals(0, snapshotSaves.get(0).getFileCount());
        assertEquals(501L, snapshotSaves.get(1).getCommitId());

        ArgumentCaptor<ProjectBranch> branchCaptor = ArgumentCaptor.forClass(ProjectBranch.class);
        verify(projectBranchRepository, org.mockito.Mockito.times(4)).save(branchCaptor.capture());
        List<ProjectBranch> branchSaves = branchCaptor.getAllValues();
        ProjectBranch mainBranch = branchSaves.stream().filter(branch -> "main".equals(branch.getName())).reduce((first, second) -> second).orElse(null);
        ProjectBranch devBranch = branchSaves.stream().filter(branch -> "dev".equals(branch.getName())).reduce((first, second) -> second).orElse(null);
        assertNotNull(mainBranch);
        assertNotNull(devBranch);
        assertEquals(501L, mainBranch.getHeadCommitId());
        assertEquals(501L, devBranch.getHeadCommitId());

        verify(projectRepositoryBootstrapSupport).ensureRepositorySnapshotInitialized(any(ProjectCodeRepository.class), org.mockito.Mockito.eq(userId));
    }
}
