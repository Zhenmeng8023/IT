package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ProjectTaskAccessSupport {
    private final ProjectTaskRepository projectTaskRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectPermissionService projectPermissionService;

    public ProjectTask getTaskOrThrow(Long taskId) {
        return projectTaskRepository.findById(taskId)
                .orElseThrow(() -> new BusinessException("任务不存在"));
    }

    public Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }

    public void assertTaskReadable(Long taskId, Long currentUserId) {
        ProjectTask task = getTaskOrThrow(taskId);
        assertProjectReadable(task.getProjectId(), currentUserId);
    }

    public void assertProjectReadable(Long projectId, Long currentUserId) {
        if (!isTaskCollaborator(projectId, currentUserId)) {
            throw new BusinessException("只有已加入项目的成员才能查看任务协作");
        }
    }

    public void assertTaskWritable(Long taskId, Long currentUserId) {
        ProjectTask task = getTaskOrThrow(taskId);
        projectPermissionService.assertProjectWritable(task.getProjectId(), currentUserId);
    }

    public boolean canManageTask(Long taskId, Long currentUserId) {
        ProjectTask task = getTaskOrThrow(taskId);
        return projectPermissionService.canManageProject(task.getProjectId(), currentUserId);
    }

    public boolean isTaskCollaborator(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            return false;
        }
        Project project = getProjectOrThrow(projectId);
        if (currentUserId.equals(project.getAuthorId())) {
            return true;
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .isPresent();
    }

    public ProjectMember getActiveMemberOrNull(Long projectId, Long userId) {
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .orElse(null);
    }
}
