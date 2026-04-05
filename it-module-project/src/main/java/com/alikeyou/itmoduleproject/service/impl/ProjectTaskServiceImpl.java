package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectTaskCreateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenApplyRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskReopenReviewRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskStatusUpdateRequest;
import com.alikeyou.itmoduleproject.dto.ProjectTaskUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.ProjectTaskReopenRequest;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskPriorityEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskReopenRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectTaskDependencyService;
import com.alikeyou.itmoduleproject.service.ProjectTaskLogService;
import com.alikeyou.itmoduleproject.service.ProjectTaskService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectTaskAccessSupport;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectTaskReopenRequestVO;
import com.alikeyou.itmoduleproject.vo.ProjectTaskVO;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ProjectTaskServiceImpl implements ProjectTaskService {

    private static final String REOPEN_PENDING = "pending";
    private static final String REOPEN_APPROVED = "approved";
    private static final String REOPEN_REJECTED = "rejected";

    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectTaskReopenRequestRepository projectTaskReopenRequestRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;
    private final ProjectTaskAccessSupport taskAccessSupport;
    private final ProjectTaskLogService projectTaskLogService;
    private final ProjectTaskDependencyService projectTaskDependencyService;
    private final ProjectActivityLogService projectActivityLogService;

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
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "create_task", "task", saved.getId(), "创建任务：" + saved.getTitle());

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
            applyStatus(task, request.getStatus(), currentUserId);
        }
        task.setDueDate(request.getDueDate());

        ProjectTask saved = projectTaskRepository.save(task);
        recordTaskChanges(saved.getId(), currentUserId, oldTitle, saved.getTitle(), oldDescription, saved.getDescription(), oldPriority, saved.getPriority(), oldAssigneeId, saved.getAssigneeId(), oldStatus, saved.getStatus(), oldDueDate, saved.getDueDate());
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "update_task", "task", saved.getId(), "更新任务：" + saved.getTitle());

        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskVO updateTaskStatus(Long taskId, ProjectTaskStatusUpdateRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        validateStatus(request.getStatus());

        boolean canManage = projectPermissionService.canManageProject(task.getProjectId(), currentUserId);
        boolean canUpdateAsAssignee = task.getAssigneeId() != null
                && task.getAssigneeId().equals(currentUserId)
                && taskAccessSupport.isTaskCollaborator(task.getProjectId(), currentUserId);
        if (!canManage && !canUpdateAsAssignee) {
            throw new BusinessException("无权修改任务状态");
        }

        String oldStatus = task.getStatus();
        String newStatus = request.getStatus();
        if (Objects.equals(oldStatus, newStatus)) {
            Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(task)));
            return toTaskVO(task, userMap);
        }

        boolean fromDoneToUnfinished = ProjectTaskStatusEnum.DONE.getValue().equals(oldStatus)
                && !ProjectTaskStatusEnum.DONE.getValue().equals(newStatus);

        if (!canManage && fromDoneToUnfinished) {
            assertAssigneeCanOperateCurrentCycle(task, currentUserId);
            throw new BusinessException("已完成任务不能直接改回未完成，请先提交重开申请并等待管理员或所有者确认");
        }

        applyStatus(task, newStatus, currentUserId);
        ProjectTask saved = projectTaskRepository.save(task);
        recordStatusChange(saved.getId(), currentUserId, oldStatus, saved.getStatus());
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "change_task_status", "task", saved.getId(),
                "任务状态变更：" + saved.getTitle() + "（" + oldStatus + " -> " + saved.getStatus() + "）");

        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskReopenRequestVO applyReopenRequest(Long taskId, ProjectTaskReopenApplyRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        taskAccessSupport.assertTaskReadable(taskId, currentUserId);
        if (!ProjectTaskStatusEnum.DONE.getValue().equals(task.getStatus())) {
            throw new BusinessException("只有已完成任务才能提交重开申请");
        }
        if (task.getAssigneeId() == null || !task.getAssigneeId().equals(currentUserId)) {
            throw new BusinessException("只有当前负责人本人可以提交重开申请");
        }
        ProjectMember currentMember = taskAccessSupport.getActiveMemberOrThrow(task.getProjectId(), currentUserId, "当前用户不是项目有效成员，无法提交重开申请");
        assertAssigneeCanOperateCurrentCycle(task, currentUserId);
        validateReopenTargetStatus(request == null ? null : request.getTargetStatus());
        String reason = requireText(request == null ? null : request.getReason(), "请填写重开原因");
        if (projectTaskReopenRequestRepository.existsByTaskIdAndStatus(taskId, REOPEN_PENDING)) {
            throw new BusinessException("该任务已有待处理的重开申请，请勿重复提交");
        }

        ProjectTaskReopenRequest saved = projectTaskReopenRequestRepository.save(ProjectTaskReopenRequest.builder()
                .taskId(task.getId())
                .projectId(task.getProjectId())
                .applicantId(currentUserId)
                .applicantMemberJoinedAt(currentMember.getJoinedAt())
                .fromStatus(task.getStatus())
                .targetStatus(request.getTargetStatus())
                .reason(reason)
                .status(REOPEN_PENDING)
                .build());

        projectTaskLogService.recordFieldChange(task.getId(), currentUserId, "reopen_request", "status", task.getStatus(), request.getTargetStatus());
        projectActivityLogService.record(task.getProjectId(), currentUserId, "request_reopen_task", "task", task.getId(),
                "提交任务重开申请：" + task.getTitle());

        return toReopenRequestVO(saved);
    }

    @Override
    public List<ProjectTaskReopenRequestVO> listReopenRequests(Long taskId, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        taskAccessSupport.assertProjectReadable(task.getProjectId(), currentUserId);
        return projectTaskReopenRequestRepository.findByTaskIdOrderByCreatedAtDesc(taskId)
                .stream()
                .map(this::toReopenRequestVO)
                .toList();
    }

    @Override
    @Transactional
    public ProjectTaskVO approveReopenRequest(Long taskId, Long requestId, ProjectTaskReopenReviewRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        projectPermissionService.assertProjectManageMembers(task.getProjectId(), currentUserId);
        ProjectTaskReopenRequest reopenRequest = projectTaskReopenRequestRepository.findByIdAndTaskId(requestId, taskId)
                .orElseThrow(() -> new BusinessException("重开申请不存在"));
        if (!REOPEN_PENDING.equalsIgnoreCase(reopenRequest.getStatus())) {
            throw new BusinessException("该重开申请已处理，不能重复审批");
        }
        String targetStatus = request != null && StringUtils.hasText(request.getApprovedTargetStatus())
                ? request.getApprovedTargetStatus().trim()
                : reopenRequest.getTargetStatus();
        validateReopenTargetStatus(targetStatus);
        String oldStatus = task.getStatus();
        if (!ProjectTaskStatusEnum.DONE.getValue().equals(oldStatus)) {
            throw new BusinessException("当前任务已不是已完成状态，不能按重开申请回退");
        }

        reopenRequest.setStatus(REOPEN_APPROVED);
        reopenRequest.setReviewerId(currentUserId);
        reopenRequest.setReviewedAt(LocalDateTime.now());
        reopenRequest.setReviewRemark(request == null ? null : request.getReviewRemark());
        reopenRequest.setTargetStatus(targetStatus);
        projectTaskReopenRequestRepository.save(reopenRequest);

        clearDoneSnapshot(task);
        task.setStatus(targetStatus);
        task.setLastReopenedBy(currentUserId);
        task.setLastReopenedAt(LocalDateTime.now());
        ProjectTask saved = projectTaskRepository.save(task);

        recordStatusChange(saved.getId(), currentUserId, oldStatus, saved.getStatus());
        projectActivityLogService.record(saved.getProjectId(), currentUserId, "approve_reopen_task", "task", saved.getId(),
                "通过任务重开申请：" + saved.getTitle());

        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(collectUserIds(List.of(saved)));
        return toTaskVO(saved, userMap);
    }

    @Override
    @Transactional
    public ProjectTaskReopenRequestVO rejectReopenRequest(Long taskId, Long requestId, ProjectTaskReopenReviewRequest request, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        projectPermissionService.assertProjectManageMembers(task.getProjectId(), currentUserId);
        ProjectTaskReopenRequest reopenRequest = projectTaskReopenRequestRepository.findByIdAndTaskId(requestId, taskId)
                .orElseThrow(() -> new BusinessException("重开申请不存在"));
        if (!REOPEN_PENDING.equalsIgnoreCase(reopenRequest.getStatus())) {
            throw new BusinessException("该重开申请已处理，不能重复审批");
        }
        reopenRequest.setStatus(REOPEN_REJECTED);
        reopenRequest.setReviewerId(currentUserId);
        reopenRequest.setReviewedAt(LocalDateTime.now());
        reopenRequest.setReviewRemark(request == null ? null : request.getReviewRemark());
        ProjectTaskReopenRequest saved = projectTaskReopenRequestRepository.save(reopenRequest);

        projectActivityLogService.record(task.getProjectId(), currentUserId, "reject_reopen_task", "task", task.getId(),
                "驳回任务重开申请：" + task.getTitle());
        return toReopenRequestVO(saved);
    }

    @Override
    @Transactional
    public void deleteTask(Long taskId, Long currentUserId) {
        ProjectTask task = taskAccessSupport.getTaskOrThrow(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
        projectTaskLogService.recordDelete(taskId, currentUserId, task.getTitle());
        projectActivityLogService.record(task.getProjectId(), currentUserId, "delete_task", "task", task.getId(), "删除任务：" + task.getTitle());
        projectTaskRepository.delete(task);
    }

    private void applyStatus(ProjectTask task, String status, Long operatorId) {
        if (ProjectTaskStatusEnum.DONE.getValue().equals(status)) {
            projectTaskDependencyService.assertTaskCanBeDone(task.getId());
            task.setCompletedAt(LocalDateTime.now());
            task.setCompletedBy(operatorId);
            ProjectMember assigneeMember = task.getAssigneeId() == null ? null : taskAccessSupport.getActiveMemberOrNull(task.getProjectId(), task.getAssigneeId());
            task.setCompletedMemberJoinedAt(assigneeMember == null ? null : assigneeMember.getJoinedAt());
        } else {
            clearDoneSnapshot(task);
        }
        task.setStatus(status);
    }

    private void clearDoneSnapshot(ProjectTask task) {
        task.setCompletedAt(null);
        task.setCompletedBy(null);
        task.setCompletedMemberJoinedAt(null);
    }

    private void assertAssigneeCanOperateCurrentCycle(ProjectTask task, Long currentUserId) {
        ProjectMember currentMember = taskAccessSupport.getActiveMemberOrThrow(task.getProjectId(), currentUserId, "当前用户不是项目有效成员，无法修改任务状态");
        if (task.getCompletedMemberJoinedAt() != null && !Objects.equals(task.getCompletedMemberJoinedAt(), currentMember.getJoinedAt())) {
            throw new BusinessException("你不能修改上一入组周期已完成的任务，请联系项目管理员或所有者处理");
        }
    }

    private void recordTaskChanges(Long taskId, Long currentUserId,
                                   String oldTitle, String newTitle,
                                   String oldDescription, String newDescription,
                                   String oldPriority, String newPriority,
                                   Long oldAssigneeId, Long newAssigneeId,
                                   String oldStatus, String newStatus,
                                   LocalDateTime oldDueDate, LocalDateTime newDueDate) {
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

    private void validateReopenTargetStatus(String status) {
        if (!StringUtils.hasText(status)) {
            throw new BusinessException("请选择重开后的目标状态");
        }
        String normalized = status.trim();
        if (!ProjectTaskStatusEnum.TODO.getValue().equals(normalized) && !ProjectTaskStatusEnum.IN_PROGRESS.getValue().equals(normalized)) {
            throw new BusinessException("重开后的目标状态只能是待处理或进行中");
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
        Set<Long> ids = new LinkedHashSet<>();
        for (ProjectTask task : tasks) {
            if (task.getAssigneeId() != null) {
                ids.add(task.getAssigneeId());
            }
            if (task.getCreatedBy() != null) {
                ids.add(task.getCreatedBy());
            }
            if (task.getCompletedBy() != null) {
                ids.add(task.getCompletedBy());
            }
        }
        return new ArrayList<>(ids);
    }

    private ProjectTaskVO toTaskVO(ProjectTask task, Map<Long, UserInfoLite> userMap) {
        return ProjectVoMapper.toProjectTaskVO(task, userMap.get(task.getAssigneeId()), userMap.get(task.getCreatedBy()));
    }

    private ProjectTaskReopenRequestVO toReopenRequestVO(ProjectTaskReopenRequest request) {
        List<Long> ids = new ArrayList<>();
        if (request.getApplicantId() != null) {
            ids.add(request.getApplicantId());
        }
        if (request.getReviewerId() != null) {
            ids.add(request.getReviewerId());
        }
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(ids);
        return ProjectVoMapper.toProjectTaskReopenRequestVO(request, userMap.get(request.getApplicantId()), userMap.get(request.getReviewerId()));
    }
}
