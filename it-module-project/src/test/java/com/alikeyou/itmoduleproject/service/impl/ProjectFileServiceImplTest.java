package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectFile;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileRepository;
import com.alikeyou.itmoduleproject.repository.ProjectFileVersionRepository;
import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.service.ProjectWorkspaceService;
import com.alikeyou.itmoduleproject.support.FileStorageService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectRepoStorageSupport;
import com.alikeyou.itmoduleproject.support.ProjectRepositoryBootstrapSupport;
import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;
import com.alikeyou.itmoduleproject.vo.ProjectWorkspaceItemVO;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectFileServiceImplTest {

    private final ProjectFileRepository projectFileRepository = mock(ProjectFileRepository.class);
    private final ProjectFileVersionRepository projectFileVersionRepository = mock(ProjectFileVersionRepository.class);
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository = mock(ProjectCodeRepositoryRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final FileStorageService fileStorageService = mock(FileStorageService.class);
    private final ProjectWorkspaceService projectWorkspaceService = mock(ProjectWorkspaceService.class);
    private final ProjectCodeRepositoryService projectCodeRepositoryService = mock(ProjectCodeRepositoryService.class);
    private final ProjectRepositoryBootstrapSupport projectRepositoryBootstrapSupport = new ProjectRepositoryBootstrapSupport(
            mock(com.alikeyou.itmoduleproject.repository.ProjectBranchRepository.class),
            mock(com.alikeyou.itmoduleproject.repository.ProjectCommitRepository.class),
            mock(com.alikeyou.itmoduleproject.repository.ProjectSnapshotRepository.class),
            mock(com.alikeyou.itmoduleproject.repository.ProjectSnapshotItemRepository.class),
            mock(ProjectFileRepository.class),
            mock(ProjectFileVersionRepository.class),
            new ProjectRepoStorageSupport(mock(com.alikeyou.itmoduleproject.repository.ProjectBlobRepository.class))
    );

    private final ProjectFileServiceImpl service = new ProjectFileServiceImpl(
            projectFileRepository,
            projectFileVersionRepository,
            projectCodeRepositoryRepository,
            projectPermissionService,
            fileStorageService,
            projectWorkspaceService,
            projectCodeRepositoryService,
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
        ProjectWorkspaceItemVO stagedDelete = ProjectWorkspaceItemVO.builder()
                .id(21L)
                .workspaceId(31L)
                .canonicalPath("/src/App.vue")
                .changeType("DELETE")
                .stagedFlag(true)
                .build();

        when(projectFileRepository.findById(11L)).thenReturn(Optional.of(projectFile));
        when(projectCodeRepositoryService.getByProjectId(1001L, 99L)).thenReturn(ProjectCodeRepositoryVO.builder()
                .id(501L)
                .projectId(1001L)
                .defaultBranchId(7L)
                .build());
        when(projectWorkspaceService.stageDelete(1001L, 7L, 99L, "/src/App.vue")).thenReturn(stagedDelete);

        ProjectWorkspaceItemVO result = service.deleteFile(11L, null, 99L);

        assertEquals(21L, result.getId());
        assertEquals("/src/App.vue", result.getCanonicalPath());
        verify(projectPermissionService).assertProjectWritable(1001L, 99L);
        verify(projectWorkspaceService).stageDelete(1001L, 7L, 99L, "/src/App.vue");
        verify(projectFileRepository, never()).delete(projectFile);
        verify(projectFileVersionRepository, never()).deleteAll(org.mockito.ArgumentMatchers.anyIterable());
        verify(fileStorageService, never()).delete(org.mockito.ArgumentMatchers.anyString());
    }
}
