package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskPriorityEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskDependencyService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectTaskLogService projectTaskLogService;
    private final ProjectTaskDependencyService projectTaskDependencyService;

    @Override
    public List<ProjectTaskVO> listTasks(Long projectId, String status, String priority, Long assigneeId, Long currentUserId) {
        taskAccessSupport.assertProjectReadable(projectId, currentUserId);
        Specification<ProjectTask> specification = (root, query, cb) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("projectId"), projectId));
            if (StringUtils.hasText(status)) {
                predicates.add(cb.equal(root.get("status"), status.trim()));
            }
            if (StringUtils.hasText(priority)) {
                predicates.add(cb.equal(root.get("priority"), priority.trim()));
            }
            if (assigneeId != null) {
                predicates.add(cb.equal(root.get("assigneeId"), assigneeId));
            }
            return cb.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        List<ProjectTask> tasks = projectTaskRepository.findAll(specification, Sort.by(Sort.Direction.DESC, "createdAt"));
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(tasks));
        return tasks.stream().map(task -> toTaskVO(task, userMap)).toList();
    }

    @Override
    public List<ProjectTaskVO> listMyTasks(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        taskAccessSupport.assertProjectReadable(projectId, currentUserId);
        List<ProjectTask> tasks = projectTaskRepository.findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(projectId, currentUserId);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(tasks));
        return tasks.stream().map(task -> toTaskVO(task, userMap)).toList();
    }

    @Override
    @Transactional
    public ProjectTaskVO createTask(ProjectTaskCreateRequest request, Long currentUserId) {
        projectPermissionService.assertProjectWritable(request.getProjectId(), currentUserId);
        validatePriority(request.getPriority());
        validateAssignee(request.getProjectId(), request.getAssigneeId());
        ProjectTask task = ProjectTask.builder()
                .projectId(request.getProjectId())
                .title(requireText(request.getTitle(), "任务标题不能为空"))
                .description(request.getDescription())
                .status(ProjectTaskStatusEnum.TODO.getValue())
                .priority(request.getPriority() == null ? ProjectTaskPriorityEnum.MEDIUM.getValue() : request.getPriority())
                .assigneeId(request.getAssigneeId())
                .dueDate(request.getDueDate())
                .createdBy(currentUserId)
                .build();
        ProjectTask saved = projectTaskRepository.save(task);
        projectTaskLogService.recordCreate(saved.getId(), currentUserId);
        if (saved.getAssigneeId() != null) {
            projectTaskLogService.recordFieldChange(saved.getId(), currentUserId, "assign", "assignee_id", null, saved.getAssigneeId());
        }
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTask(Long taskId, ProjectTaskUpdateRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);

        String oldTitle = task.getTitle();
        String oldDescription = task.getDescription();
        String oldPriority = task.getPriority();
        Long oldAssigneeId = task.getAssigneeId();
        String oldStatus = task.getStatus();
        LocalDateTime oldDueDate = task.getDueDate();

        if (request.getTitle() != null) {
            task.setTitle(requireText(request.getTitle(), "任务标题不能为空"));
        }
        if (request.getDescription() != null) {
            task.setDescription(request.getDescription());
        }
        if (request.getPriority() != null) {
            validatePriority(request.getPriority());
            task.setPriority(request.getPriority());
        }
        if (request.getAssigneeId() != null) {
            validateAssignee(task.getProjectId(), request.getAssigneeId());
            task.setAssigneeId(request.getAssigneeId());
        }
        if (request.getStatus() != null) {
            validateStatus(request.getStatus());
            applyStatus(task, request.getStatus());
        }
        task.setDueDate(request.getDueDate());

        ProjectTask saved = projectTaskRepository.save(task);
        recordTaskChanges(saved.getId(), currentUserId, oldTitle, saved.getTitle(), oldDescription, saved.getDescription(), oldPriority, saved.getPriority(), oldAssigneeId, saved.getAssigneeId(), oldStatus, saved.getStatus(), oldDueDate, saved.getDueDate());
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        boolean canManage = projectPermissionService.canManageProject(task.getProjectId(), currentUserId);
        boolean canUpdateAsAssignee = task.getAssigneeId() != null && task.getAssigneeId().equals(currentUserId) && taskAccessSupport.isTaskCollaborator(task.getProjectId(), currentUserId);
        if (!canManage && !canUpdateAsAssignee) {
            throw new BusinessException("无权修改任务状态");
        }
        validateStatus(request.getStatus());
        String oldStatus = task.getStatus();
        applyStatus(task, request.getStatus());
        ProjectTask saved = projectTaskRepository.save(task);
        recordStatusChange(saved.getId(), currentUserId, oldStatus, saved.getStatus());
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
        projectTaskLogService.recordDelete(taskId, currentUserId, task.getTitle());
        projectTaskRepository.delete(task);
    }

    private void applyStatus(ProjectTask task, String status) {
        if (ProjectTaskStatusEnum.DONE.getValue().equals(status)) {
            projectTaskDependencyService.assertTaskCanBeDone(task.getId());
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        task.setStatus(status);
    }

    private void recordTaskChanges(Long taskId, Long currentUserId, String oldTitle, String newTitle, String oldDescription, String newDescription, String oldPriority, String newPriority, Long oldAssigneeId, Long newAssigneeId, String oldStatus, String newStatus, LocalDateTime oldDueDate, LocalDateTime newDueDate) {
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "title", oldTitle, newTitle);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "description", oldDescription, newDescription);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "change_priority", "priority", oldPriority, newPriority);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "assign", "assignee_id", oldAssigneeId, newAssigneeId);
        projectTaskLogService.recordFieldChange(taskId, currentUserId, "update", "due_date", oldDueDate, newDueDate);
        if (!Objects.equals(oldStatus, newStatus)) {
            recordStatusChange(taskId, currentUserId, oldStatus, newStatus);
        }
    }

    private void recordStatusChange(Long taskId, Long currentUserId, String oldStatus, String newStatus) {
        String action = "change_status";
        if (!ProjectTaskStatusEnum.DONE.getValue().equals(oldStatus) && ProjectTaskStatusEnum.DONE.getValue().equals(newStatus)) {
            action = "complete";
        } else if (ProjectTaskStatusEnum.DONE.getValue().equals(oldStatus) && !ProjectTaskStatusEnum.DONE.getValue().equals(newStatus)) {
            action = "reopen";
        }
        projectTaskLogService.recordFieldChange(taskId, currentUserId, action, "status", oldStatus, newStatus);
    }

    private void validatePriority(String priority) {
        if (priority != null && !ProjectTaskPriorityEnum.contains(priority)) {
            throw new BusinessException("任务优先级不合法");
        }
    }

    private void validateStatus(String status) {
        if (!ProjectTaskStatusEnum.contains(status)) {
            throw new BusinessException("任务状态不合法");
        }
    }

    private void validateAssignee(Long projectId, Long assigneeId) {
        if (assigneeId == null) {
            return;
        }
        UserInfoLite user = userInfoLiteRepository.findById(assigneeId)
                .orElseThrow(() -> new BusinessException("负责人用户不存在"));
        if (StringUtils.hasText(user.getStatus()) && !"active".equalsIgnoreCase(user.getStatus().trim())) {
            throw new BusinessException("负责人用户状态不可用");
        }
        Project project = projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
        if (assigneeId.equals(project.getAuthorId())) {
            return;
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, assigneeId)
                .orElseThrow(() -> new BusinessException("负责人必须是当前项目的有效成员"));
        if (!ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus())) {
            throw new BusinessException("负责人必须是当前项目的有效成员");
        }
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private List<Long> collectUserIds(List<ProjectTask> tasks) {
        List<Long> ids = new ArrayList<>();
        for (ProjectTask task : tasks) {
            if (task.getAssigneeId() != null) {
                ids.add(task.getAssigneeId());
            }
            if (task.getCreatedBy() != null) {
                ids.add(task.getCreatedBy());
            }
        }
        return ids;
    }

    private ProjectTaskVO toTaskVO(ProjectTask task, Map<Long, UserInfoLite> userMap) {
        return ProjectVoMapper.toProjectTaskVO(task, userMap.get(task.getAssigneeId()), userMap.get(task.getCreatedBy()));
    }
}
