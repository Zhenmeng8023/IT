package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.entity.ProjectMilestone;
import com.alikeyou.itmoduleproject.entity.ProjectCodeRepository;
import com.alikeyou.itmoduleproject.entity.ProjectCommit;
import com.alikeyou.itmoduleproject.repository.ProjectCodeRepositoryRepository;
import com.alikeyou.itmoduleproject.repository.ProjectCommitRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMilestoneRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectMilestoneService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectMilestoneServiceImpl implements ProjectMilestoneService {

    private static final Set<String> ALLOWED_STATUS = Set.of("planned", "active", "completed", "cancelled");

    private final ProjectMilestoneRepository projectMilestoneRepository;
    private final ProjectRepository projectRepository;
    private final ProjectCodeRepositoryRepository projectCodeRepositoryRepository;
    private final ProjectCommitRepository projectCommitRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    public List<ProjectMilestone> listMilestones(Long projectId, String status, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        if (!StringUtils.hasText(status)) {
            return projectMilestoneRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        }
        return projectMilestoneRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status.trim());
    }

    @Override
    @Transactional
    public ProjectMilestone createMilestone(ProjectMilestone request, Long currentUserId) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        assertProjectExists(request.getProjectId());
        validateName(request.getName());
        validateStatus(request.getStatus());
        ProjectCodeRepository repository = resolveRepository(request.getProjectId());
        ProjectMilestone entity = ProjectMilestone.builder()
                .projectId(request.getProjectId())
                .repositoryId(repository == null ? null : repository.getId())
                .branchId(request.getBranchId())
                .anchorCommitId(request.getAnchorCommitId())
                .fromCommitId(request.getFromCommitId())
                .toCommitId(request.getToCommitId())
                .sortOrder(request.getSortOrder())
                .name(request.getName().trim())
                .description(request.getDescription())
                .status(normalizeStatus(request.getStatus()))
                .startDate(request.getStartDate())
                .dueDate(request.getDueDate())
                .completedAt(resolveCompletedAt(normalizeStatus(request.getStatus()), request.getCompletedAt()))
                .createdBy(currentUserId)
                .build();
        applyCommitBinding(entity, repository);
        ProjectMilestone saved = projectMilestoneRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "create_milestone", "milestone", saved.getId(), "创建里程碑：" + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public ProjectMilestone updateMilestone(Long id, ProjectMilestone request, Long currentUserId) {
        ProjectMilestone entity = getMilestoneOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        validateName(request.getName());
        validateStatus(request.getStatus());
        ProjectCodeRepository repository = resolveRepository(entity.getProjectId());
        entity.setName(request.getName().trim());
        entity.setDescription(request.getDescription());
        entity.setStartDate(request.getStartDate());
        entity.setDueDate(request.getDueDate());
        entity.setStatus(normalizeStatus(request.getStatus()));
        entity.setCompletedAt(resolveCompletedAt(entity.getStatus(), request.getCompletedAt()));
        entity.setRepositoryId(repository == null ? null : repository.getId());
        entity.setBranchId(request.getBranchId());
        entity.setAnchorCommitId(request.getAnchorCommitId());
        entity.setFromCommitId(request.getFromCommitId());
        entity.setToCommitId(request.getToCommitId());
        entity.setSortOrder(request.getSortOrder());
        applyCommitBinding(entity, repository);
        ProjectMilestone saved = projectMilestoneRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "update_milestone", "milestone", saved.getId(), "更新里程碑：" + saved.getName());
        return saved;
    }

    @Override
    @Transactional
    public ProjectMilestone changeStatus(Long id, String status, Long currentUserId) {
        ProjectMilestone entity = getMilestoneOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        validateStatus(status);
        entity.setStatus(status.trim());
        entity.setCompletedAt(resolveCompletedAt(entity.getStatus(), entity.getCompletedAt()));
        ProjectMilestone saved = projectMilestoneRepository.save(entity);
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "change_milestone_status", "milestone", saved.getId(), "里程碑状态变更：" + saved.getName() + " -> " + saved.getStatus());
        return saved;
    }

    @Override
    @Transactional
    public void deleteMilestone(Long id, Long currentUserId) {
        ProjectMilestone entity = getMilestoneOrThrow(id);
        projectPermissionService.assertProjectManageMembers(entity.getProjectId(), currentUserId);
        projectActivityLogService.record(entity.getProjectId(), currentUserId, "delete_milestone", "milestone", entity.getId(), "删除里程碑：" + entity.getName());
        projectMilestoneRepository.delete(entity);
    }

    @Override
    public Map<String, Object> getOverview(Long projectId, Long currentUserId) {
        assertProjectExists(projectId);
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("total", projectMilestoneRepository.countByProjectId(projectId));
        map.put("totalCount", projectMilestoneRepository.countByProjectId(projectId));
        map.put("plannedCount", projectMilestoneRepository.countByProjectIdAndStatus(projectId, "planned"));
        map.put("activeCount", projectMilestoneRepository.countByProjectIdAndStatus(projectId, "active"));
        map.put("completedCount", projectMilestoneRepository.countByProjectIdAndStatus(projectId, "completed"));
        map.put("cancelledCount", projectMilestoneRepository.countByProjectIdAndStatus(projectId, "cancelled"));
        ProjectMilestone active = projectMilestoneRepository.findFirstByProjectIdAndStatusOrderByDueDateAscIdAsc(projectId, "active").orElse(null);
        ProjectMilestone planned = projectMilestoneRepository.findFirstByProjectIdAndStatusOrderByDueDateAscIdAsc(projectId, "planned").orElse(null);
        map.put("nearest", active != null ? active : planned);
        return map;
    }

    private ProjectMilestone getMilestoneOrThrow(Long id) {
        return projectMilestoneRepository.findById(id).orElseThrow(() -> new BusinessException("里程碑不存在"));
    }

    private void assertProjectExists(Long projectId) {
        if (!projectRepository.existsById(projectId)) {
            throw new BusinessException("项目不存在");
        }
    }

    private void validateName(String name) {
        if (!StringUtils.hasText(name)) {
            throw new BusinessException("里程碑名称不能为空");
        }
    }

    private void validateStatus(String status) {
        String value = normalizeStatus(status);
        if (!ALLOWED_STATUS.contains(value)) {
            throw new BusinessException("里程碑状态不合法");
        }
    }

    private String normalizeStatus(String status) {
        return StringUtils.hasText(status) ? status.trim() : "planned";
    }

    private LocalDateTime resolveCompletedAt(String status, LocalDateTime completedAt) {
        if ("completed".equals(status)) {
            return completedAt == null ? LocalDateTime.now() : completedAt;
        }
        return null;
    }

    private ProjectCodeRepository resolveRepository(Long projectId) {
        return projectCodeRepositoryRepository.findByProjectId(projectId).orElse(null);
    }

    private void applyCommitBinding(ProjectMilestone entity, ProjectCodeRepository repository) {
        if (entity == null) {
            return;
        }
        if (entity.getAnchorCommitId() == null && entity.getToCommitId() != null) {
            entity.setAnchorCommitId(entity.getToCommitId());
        }
        if (entity.getFromCommitId() == null && entity.getAnchorCommitId() != null) {
            entity.setFromCommitId(entity.getAnchorCommitId());
        }
        if (entity.getToCommitId() == null && entity.getAnchorCommitId() != null && "completed".equals(entity.getStatus())) {
            entity.setToCommitId(entity.getAnchorCommitId());
        }
        validateCommitBelongsToRepository(repository, entity.getAnchorCommitId(), "锚点提交");
        validateCommitBelongsToRepository(repository, entity.getFromCommitId(), "起始提交");
        validateCommitBelongsToRepository(repository, entity.getToCommitId(), "结束提交");
    }

    private void validateCommitBelongsToRepository(ProjectCodeRepository repository, Long commitId, String label) {
        if (commitId == null) {
            return;
        }
        if (repository == null) {
            throw new BusinessException(label + "不存在，请先初始化项目仓库");
        }
        ProjectCommit commit = projectCommitRepository.findById(commitId)
                .orElseThrow(() -> new BusinessException(label + "不存在"));
        if (!repository.getId().equals(commit.getRepositoryId())) {
            throw new BusinessException(label + "不属于当前项目仓库");
        }
    }
}
