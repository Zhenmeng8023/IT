package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectTaskPriorityEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;

    @Override
    public List<ProjectTaskVO> listTasks(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        List<ProjectTask> tasks = projectTaskRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(tasks));
        return tasks.stream()
                .map(task -> toTaskVO(task, userMap))
                .toList();
    }

    @Override
    @Transactional
    public ProjectTaskVO createTask(ProjectTaskCreateRequest request, Long currentUserId) {
        projectPermissionService.assertProjectWritable(request.getProjectId(), currentUserId);
        validatePriority(request.getPriority());
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
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(List.of(saved.getAssigneeId(), saved.getCreatedBy()));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTask(Long taskId, ProjectTaskUpdateRequest request, Long currentUserId) {
        ProjectTask task = getTask(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
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
            task.setAssigneeId(request.getAssigneeId());
        }
        task.setDueDate(request.getDueDate());
        ProjectTask saved = projectTaskRepository.save(task);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(List.of(saved.getAssigneeId(), saved.getCreatedBy()));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId) {
        ProjectTask task = getTask(taskId);
        if (!(projectPermissionService.canManageProject(task.getProjectId(), currentUserId) ||
                (task.getAssigneeId() != null && task.getAssigneeId().equals(currentUserId)))) {
            throw new BusinessException("无权修改任务状态");
        }
        validateStatus(request.getStatus());
        task.setStatus(request.getStatus());
        if (ProjectTaskStatusEnum.DONE.getValue().equals(request.getStatus())) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
        ProjectTask saved = projectTaskRepository.save(task);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(List.of(saved.getAssigneeId(), saved.getCreatedBy()));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        ProjectTask task = getTask(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
        projectTaskRepository.delete(task);
    }

    private ProjectTask getTask(Long taskId) {
        return projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
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

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new BusinessException(message);
        }
        return value.trim();
    }

    private List<Long> collectUserIds(List<ProjectTask> tasks) {
        List<Long> ids = new ArrayList<>();
        for (ProjectTask task : tasks) {
            ids.add(task.getAssigneeId());
            ids.add(task.getCreatedBy());
        }
        return ids;
    }

    private ProjectTaskVO toTaskVO(ProjectTask task, Map<Long, UserInfoLite> userMap) {
        return ProjectVoMapper.toProjectTaskVO(
                task,
                userMap.get(task.getAssigneeId()),
                userMap.get(task.getCreatedBy())
        );
    }
}
