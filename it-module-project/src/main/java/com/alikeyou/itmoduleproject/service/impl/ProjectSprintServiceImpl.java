package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectSprint;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectSprintRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectSprintService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectSprintServiceImpl implements ProjectSprintService {

    private static final Set<String> STATUS_SET = Set.of("planned", "active", "completed", "cancelled");

    private final ProjectSprintRepository projectSprintRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    public List<ProjectSprint> listSprints(Long projectId, String status, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        if (!StringUtils.hasText(status)) {
            return projectSprintRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        validateStatus(status);
        return projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status.trim());
    }

    @Override
    @Transactional
    public ProjectSprint createSprint(ProjectSprint request, Long currentUserId) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        assertProjectExists(request.getProjectId());
        validateName(request.getName());
        validateStatus(request.getStatus());
        ProjectSprint entity = ProjectSprint.builder()
                .projectId(request.getProjectId())
                .name(request.getName().trim())
                .goal(request.getGoal())
                .status(normalizeStatus(request.getStatus()))
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .createdBy(currentUserId)
                .build();
        if ("active".equals(entity.getStatus())) {
            ensureNoOtherActiveSprint(entity.getProjectId(), null);
        }
        ProjectSprint saved = projectSprintRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "create_sprint", "sprint", saved.getId(), "创建迭代：" + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public ProjectSprint updateSprint(Long id, ProjectSprint request, Long currentUserId) {
        ProjectSprint entity = getSprintOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        validateName(request.getName());
        validateStatus(request.getStatus());
        if ("active".equals(normalizeStatus(request.getStatus()))) {
            ensureNoOtherActiveSprint(entity.getProjectId(), entity.getId());
        }
        entity.setName(request.getName().trim());
        entity.setGoal(request.getGoal());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setStartDate(request.getStartDate());
        entity.setEndDate(request.getEndDate());
        ProjectSprint saved = projectSprintRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "update_sprint", "sprint", saved.getId(), "更新迭代：" + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public ProjectSprint changeStatus(Long id, String status, Long currentUserId) {
        ProjectSprint entity = getSprintOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        validateStatus(status);
        if ("active".equals(status.trim())) {
            ensureNoOtherActiveSprint(entity.getProjectId(), entity.getId());
        }
        entity.setStatus(status.trim());
        ProjectSprint saved = projectSprintRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "change_sprint_status", "sprint", saved.getId(), "迭代状态变更：" + saved.getName() + " -> " + saved.getStatus());
        return saved;
    }

    @Override
    public Map<String, Object> getCurrentSprintSummary(Long projectId, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("current", projectSprintRepository.findFirstByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "active").orElse(null));
        map.put("plannedCount", projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "planned").size());
        map.put("activeCount", projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "active").size());
        map.put("completedCount", projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "completed").size());
        map.put("cancelledCount", projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "cancelled").size());
        return map;
    }

    private ProjectSprint getSprintOrThrow(Long id) {
        return projectSprintRepository.findById(id).orElseThrow(() -> new BusinessException("迭代不存在"));
    }

    private void assertProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException("项目不存在");
        }
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("迭代名称不能为空");
        }
    }

    private void validateStatus(String status) {
        if (!STATUS_SET.contains(normalizeStatus(status))) {
            throw new BusinessException("迭代状态不合法");
        }
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "planned";
    }

    private void ensureNoOtherActiveSprint(Long projectId, Long currentId) {
        List<ProjectSprint> list = projectSprintRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, "active");
        boolean conflict = list.stream().anyMatch(item -> currentId == null || !currentId.equals(item.getId()));
        if (conflict) {
            throw new BusinessException("同一项目同一时间只能有一个进行中的迭代");
        }
    }
}
