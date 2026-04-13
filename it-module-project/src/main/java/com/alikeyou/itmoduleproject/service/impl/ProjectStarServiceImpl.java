package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublisher;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectStar;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectStarRepository;
import com.alikeyou.itmoduleproject.service.ProjectStarService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.PageResult;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectStarStatusVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProjectStarServiceImpl implements ProjectStarService {

    private final ProjectRepository projectRepository;
    private final ProjectStarRepository projectStarRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public ProjectStarStatusVO starProject(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));

        projectPermissionService.assertProjectReadable(projectId, currentUserId);

        if (projectStarRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            return ProjectStarStatusVO.builder()
                    .projectId(projectId)
                    .starred(true)
                    .stars(project.getStars() == null ? 0 : project.getStars())
                    .build();
        }

        projectStarRepository.save(ProjectStar.builder()
                .projectId(projectId)
                .userId(currentUserId)
                .build());

        int currentStars = project.getStars() == null ? 0 : project.getStars();
        project.setStars(currentStars + 1);
        projectRepository.save(project);
        publishProjectStarNotification(project, currentUserId);

        return ProjectStarStatusVO.builder()
                .projectId(projectId)
                .starred(true)
                .stars(project.getStars())
                .build();
    }

    @Override
    @Transactional
    public ProjectStarStatusVO unstarProject(Long projectId, Long currentUserId) {
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));

        if (!projectStarRepository.existsByProjectIdAndUserId(projectId, currentUserId)) {
            return ProjectStarStatusVO.builder()
                    .projectId(projectId)
                    .starred(false)
                    .stars(project.getStars() == null ? 0 : project.getStars())
                    .build();
        }

        projectStarRepository.deleteByProjectIdAndUserId(projectId, currentUserId);

        int currentStars = project.getStars() == null ? 0 : project.getStars();
        project.setStars(Math.max(currentStars - 1, 0));
        projectRepository.save(project);

        return ProjectStarStatusVO.builder()
                .projectId(projectId)
                .starred(false)
                .stars(project.getStars())
                .build();
    }

    @Override
    public PageResult<ProjectListVO> pageMyStarredProjects(Long currentUserId, int page, int size) {
        Pageable pageable = PageRequest.of(
                Math.max(page - 1, 0),
                Math.max(size, 1),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Page<ProjectStar> starPage = projectStarRepository.findByUserIdOrderByCreatedAtDesc(currentUserId, pageable);

        List<Long> projectIds = starPage.getContent().stream()
                .map(ProjectStar::getProjectId)
                .toList();

        List<Project> projects = projectIds.isEmpty() ? List.of() : projectRepository.findAllById(projectIds);
        Map<Long, Project> projectMap = projects.stream()
                .collect(Collectors.toMap(Project::getId, Function.identity(), (a, b) -> a));

        Map<Long, UserInfoLite> authorMap = projectUserAssembler.mapByIds(
                projects.stream().map(Project::getAuthorId).filter(Objects::nonNull).distinct().toList()
        );

        List<ProjectListVO> list = projectIds.stream()
                .map(projectMap::get)
                .filter(Objects::nonNull)
                .map(project -> {
                    ProjectListVO vo = ProjectVoMapper.toProjectListVO(project, authorMap.get(project.getAuthorId()));
                    vo.setStarred(true);
                    return vo;
                })
                .toList();

        return new PageResult<>(list, starPage.getTotalElements(), page, size);
    }

    @Override
    public boolean isStarred(Long projectId, Long currentUserId) {
        if (currentUserId == null || projectId == null) {
            return false;
        }
        return projectStarRepository.existsByProjectIdAndUserId(projectId, currentUserId);
    }

    @Override
    public Set<Long> findStarredProjectIds(Long currentUserId, Collection<Long> projectIds) {
        if (currentUserId == null || projectIds == null || projectIds.isEmpty()) {
            return Set.of();
        }
        return projectStarRepository.findByUserIdAndProjectIdIn(currentUserId, projectIds)
                .stream()
                .map(ProjectStar::getProjectId)
                .collect(Collectors.toSet());
    }

    private void publishProjectStarNotification(Project project, Long actorId) {
        if (project == null || actorId == null || project.getAuthorId() == null || Objects.equals(project.getAuthorId(), actorId)) {
            return;
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("projectId", project.getId());
        payload.put("projectName", project.getName());
        payload.put("targetTitle", project.getName());
        notificationPublisher.publish(NotificationCreateCommand.builder()
                .receiverId(project.getAuthorId())
                .senderId(actorId)
                .category("project")
                .type("project_star")
                .title("项目被收藏")
                .content("收藏了你的项目《" + safeProjectName(project) + "》")
                .targetType("project")
                .targetId(project.getId())
                .sourceType("project_star")
                .sourceId(project.getId())
                .eventKey("project_star:" + project.getId() + ":user:" + actorId)
                .actionUrl("/projectdetail?projectId=" + project.getId())
                .businessStatus("open")
                .payload(payload)
                .build());
    }

    private String safeProjectName(Project project) {
        if (project == null || !StringUtils.hasText(project.getName())) {
            return "相关项目";
        }
        return project.getName();
    }
}
