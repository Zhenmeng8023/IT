package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectCreateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectVisibilityEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectCodeRepositoryService;
import com.alikeyou.itmoduleproject.service.ProjectDownloadRecordService;
import com.alikeyou.itmoduleproject.service.ProjectFileService;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.service.ProjectMilestoneService;
import com.alikeyou.itmoduleproject.service.ProjectReleaseService;
import com.alikeyou.itmoduleproject.service.ProjectSprintService;
import com.alikeyou.itmoduleproject.service.ProjectStarService;
import com.alikeyou.itmoduleproject.service.ProjectStatService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectHardDeleteSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.vo.ProjectCodeRepositoryVO;
import com.alikeyou.itmoduleproject.vo.ProjectDetailVO;
import com.alikeyou.itmoduleproject.vo.ProjectFileVO;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProjectServiceImplTest {

    private final ProjectRepository projectRepository = mock(ProjectRepository.class);
    private final ProjectMemberRepository projectMemberRepository = mock(ProjectMemberRepository.class);
    private final ProjectPermissionService projectPermissionService = mock(ProjectPermissionService.class);
    private final ProjectMemberService projectMemberService = mock(ProjectMemberService.class);
    private final ProjectCodeRepositoryService projectCodeRepositoryService = mock(ProjectCodeRepositoryService.class);
    private final ProjectTaskService projectTaskService = mock(ProjectTaskService.class);
    private final ProjectFileService projectFileService = mock(ProjectFileService.class);
    private final ProjectUserAssembler projectUserAssembler = mock(ProjectUserAssembler.class);
    private final ProjectStarService projectStarService = mock(ProjectStarService.class);
    private final ProjectMilestoneService projectMilestoneService = mock(ProjectMilestoneService.class);
    private final ProjectReleaseService projectReleaseService = mock(ProjectReleaseService.class);
    private final ProjectSprintService projectSprintService = mock(ProjectSprintService.class);
    private final ProjectDownloadRecordService projectDownloadRecordService = mock(ProjectDownloadRecordService.class);
    private final ProjectStatService projectStatService = mock(ProjectStatService.class);
    private final ProjectHardDeleteSupport projectHardDeleteSupport = mock(ProjectHardDeleteSupport.class);

    private final ProjectServiceImpl service = spy(new ProjectServiceImpl(
            projectRepository,
            projectMemberRepository,
            projectPermissionService,
            projectMemberService,
            projectCodeRepositoryService,
            projectTaskService,
            projectFileService,
            projectUserAssembler,
            projectStarService,
            projectMilestoneService,
            projectReleaseService,
            projectSprintService,
            projectDownloadRecordService,
            projectStatService,
            projectHardDeleteSupport
    ));

    @Test
    void createProject_shouldInitializeRepositoryBeforeLoadingDetail() {
        ProjectCreateRequest request = new ProjectCreateRequest();
        request.setName("demo");
        request.setVisibility(ProjectVisibilityEnum.PUBLIC.getValue());
        request.setStatus(ProjectStatusEnum.DRAFT.getValue());

        Project savedProject = Project.builder()
                .id(100L)
                .name("demo")
                .authorId(42L)
                .build();
        ProjectCodeRepositoryVO repoVO = ProjectCodeRepositoryVO.builder()
                .id(200L)
                .projectId(100L)
                .defaultBranchId(300L)
                .headCommitId(400L)
                .build();
        ProjectDetailVO detailVO = ProjectDetailVO.builder()
                .id(100L)
                .name("demo")
                .members(List.of())
                .tasks(List.of())
                .files(List.of())
                .contributors(List.of())
                .relatedProjects(List.of())
                .milestoneOverview(Map.of())
                .latestReleaseSummary(Map.of())
                .currentSprintSummary(Map.of())
                .downloadSummary(Map.of())
                .statOverview(Map.of())
                .build();

        when(projectRepository.save(any(Project.class))).thenReturn(savedProject);
        when(projectMemberRepository.save(any(ProjectMember.class))).thenAnswer(invocation -> invocation.getArgument(0));
        doNothing().when(projectPermissionService).assertProjectReadable(100L, 42L);
        when(projectMemberService.listMembers(100L, 42L)).thenReturn(List.of());
        when(projectTaskService.listTasks(100L, 42L)).thenReturn(List.of());
        when(projectFileService.listFiles(100L, 42L)).thenReturn(List.of());
        when(projectUserAssembler.mapByIds(anyCollection())).thenReturn(Collections.emptyMap());
        when(projectStarService.isStarred(100L, 42L)).thenReturn(false);
        doReturn(List.of()).when(service).listProjectContributors(100L, 42L);
        doReturn(List.of()).when(service).listRelatedProjects(100L, 42L, 6);
        when(projectMilestoneService.getOverview(100L, 42L)).thenReturn(Map.of());
        when(projectReleaseService.getLatestReleaseSummary(100L, 42L)).thenReturn(Map.of());
        when(projectSprintService.getCurrentSprintSummary(100L, 42L)).thenReturn(Map.of());
        when(projectDownloadRecordService.getSummary(100L, 42L)).thenReturn(Map.of());
        when(projectStatService.getOverview(100L, 42L)).thenReturn(Map.of());
        doReturn(detailVO).when(service).getProjectDetail(100L, 42L);
        when(projectCodeRepositoryService.initRepository(100L, 42L)).thenReturn(repoVO);

        ProjectDetailVO result = service.createProject(request, 42L);

        assertNotNull(result);
        assertEquals(100L, result.getId());
        verify(projectCodeRepositoryService).initRepository(100L, 42L);
    }

    @Test
    void deleteProject_shouldDelegateToHardDeleteSupport() {
        doNothing().when(projectPermissionService).assertProjectOwner(100L, 42L);
        doNothing().when(projectHardDeleteSupport).hardDeleteProject(100L);

        service.deleteProject(100L, 42L);

        verify(projectPermissionService).assertProjectOwner(100L, 42L);
        verify(projectHardDeleteSupport).hardDeleteProject(100L);
    }
}
