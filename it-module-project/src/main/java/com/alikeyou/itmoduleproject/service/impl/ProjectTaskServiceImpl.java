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
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import jakarta.persistence.criteria.Predicate;
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
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;

    @Override
    public List<ProjectTaskVO> listTasks(Long projectId, String status, String priority, Long assigneeId, Long currentUserId) {
        assertTaskCollaborationReadable(projectId, currentUserId);

        Specification<ProjectTask> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
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

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        List<ProjectTask> tasks = projectTaskRepository.findAll(
                specification,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(tasks));
        return tasks.stream().map(task -> toTaskVO(task, userMap)).toList();
    }

    @Override
    public List<ProjectTaskVO> listMyTasks(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }

        assertTaskCollaborationReadable(projectId, currentUserId);

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
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
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
            validateAssignee(task.getProjectId(), request.getAssigneeId());
            task.setAssigneeId(request.getAssigneeId());
        }

        if (request.getStatus() != null) {
            assertCanChangeTaskStatus(task, request.getStatus(), currentUserId);
            applyStatus(task, request.getStatus());
        }

        task.setDueDate(request.getDueDate());

        ProjectTask saved = projectTaskRepository.save(task);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId) {
        ProjectTask task = getTask(taskId);
        assertCanChangeTaskStatus(task, request.getStatus(), currentUserId);
        applyStatus(task, request.getStatus());

        ProjectTask saved = projectTaskRepository.save(task);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        ProjectTask task = getTask(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
        projectTaskRepository.delete(task);
    }

    private void assertCanChangeTaskStatus(ProjectTask task, String targetStatus, Long currentUserId) {
        validateStatus(targetStatus);

        boolean canManage = projectPermissionService.canManageProject(task.getProjectId(), currentUserId);
        if (canManage) {
            return;
        }

        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }

        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(currentUserId)) {
            throw new BusinessException("无权修改任务状态");
        }

        ProjectMember activeMember = getActiveMember(task.getProjectId(), currentUserId)
                .orElseThrow(() -> new BusinessException("无权修改任务状态"));

        if (isOldDoneTaskLockedForCurrentJoinCycle(task, activeMember)) {
            throw new BusinessException("该任务已在你上一次加入项目期间完成，当前不能修改其完成状态");
        }
    }

    private boolean isOldDoneTaskLockedForCurrentJoinCycle(ProjectTask task, ProjectMember activeMember) {
        if (task == null || activeMember == null) {
            return false;
        }
        if (!ProjectTaskStatusEnum.DONE.getValue().equals(task.getStatus())) {
            return false;
        }
        if (task.getCompletedAt() == null || activeMember.getJoinedAt() == null) {
            return false;
        }
        return task.getCompletedAt().isBefore(activeMember.getJoinedAt());
    }

    private void applyStatus(ProjectTask task, String status) {
        task.setStatus(status);
        if (ProjectTaskStatusEnum.DONE.getValue().equals(status)) {
            task.setCompletedAt(LocalDateTime.now());
        } else {
            task.setCompletedAt(null);
        }
    }

    private void assertTaskCollaborationReadable(Long projectId, Long currentUserId) {
        if (!isTaskCollaborator(projectId, currentUserId)) {
            throw new BusinessException("只有已加入项目的成员才能查看任务协作");
        }
    }

    private boolean isTaskCollaborator(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }

        Project project = getProject(projectId);
        if (currentUserId.equals(project.getAuthorId())) {
            return true;
        }

        return getActiveMember(projectId, currentUserId).isPresent();
    }

    private Optional<ProjectMember> getActiveMember(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()));
    }

    private ProjectTask getTask(Long taskId) {
        return projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
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

        Project project = getProject(projectId);
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
        return ProjectVoMapper.toProjectTaskVO(
                task,
                userMap.get(task.getAssigneeId()),
                userMap.get(task.getCreatedBy())
        );
    }
}