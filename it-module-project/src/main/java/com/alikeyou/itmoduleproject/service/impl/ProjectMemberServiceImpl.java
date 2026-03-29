package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.ProjectTask;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectTaskStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.ProjectTaskRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectUserAssembler;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectTaskRepository projectTaskRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectPermissionService projectPermissionService;
    private final ProjectUserAssembler projectUserAssembler;

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        List<ProjectMember> members = projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId);
        Map<Long, UserInfoLite> userMap = projectUserAssembler.mapByIds(
                members.stream().map(ProjectMember::getUserId).toList()
        );
        return members.stream()
                .map(member -> ProjectVoMapper.toProjectMemberVO(member, userMap.get(member.getUserId())))
                .toList();
    }

    @Override
    @Transactional
    public ProjectMemberVO addMember(ProjectMemberAddRequest request, Long currentUserId) {
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        validateRole(request.getRole());
        Project project = getProjectOrThrow(request.getProjectId());
        assertCanGrantRole(project, currentUserId, request.getRole());
        if (projectMemberRepository.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId())
                || request.getUserId().equals(project.getAuthorId())) {
            throw new BusinessException("该用户已在项目成员中");
        }
        UserInfoLite user = getActiveUserOrThrow(request.getUserId());
        ProjectMember saved = projectMemberRepository.save(ProjectMember.builder()
                .projectId(request.getProjectId())
                .userId(request.getUserId())
                .role(request.getRole())
                .status(ProjectMemberStatusEnum.ACTIVE.getValue())
                .build());
        return ProjectVoMapper.toProjectMemberVO(saved, user);
    }

    @Override
    @Transactional
    public ProjectMemberVO updateMemberRole(ProjectMemberRoleUpdateRequest request, Long currentUserId) {
        validateRole(request.getRole());
        ProjectMember member = projectMemberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("项目成员不存在"));
        projectPermissionService.assertProjectManageMembers(member.getProjectId(), currentUserId);
        Project project = getProjectOrThrow(member.getProjectId());
        assertTargetRoleManageable(project, currentUserId, member.getRole());
        assertCanGrantRole(project, currentUserId, request.getRole());
        if (request.getRole().equals(member.getRole())) {
            UserInfoLite user = projectUserAssembler.mapByIds(List.of(member.getUserId())).get(member.getUserId());
            return ProjectVoMapper.toProjectMemberVO(member, user);
        }
        member.setRole(request.getRole());
        ProjectMember saved = projectMemberRepository.save(member);
        UserInfoLite user = projectUserAssembler.mapByIds(List.of(saved.getUserId())).get(saved.getUserId());
        return ProjectVoMapper.toProjectMemberVO(saved, user);
    }

    @Override
    @Transactional
    public void removeMember(Long memberId, Long currentUserId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException("项目成员不存在"));
        projectPermissionService.assertProjectManageMembers(member.getProjectId(), currentUserId);
        Project project = getProjectOrThrow(member.getProjectId());
        assertTargetRoleManageable(project, currentUserId, member.getRole());
        handleTasksAfterMemberDeparture(member.getProjectId(), member.getUserId());
        projectMemberRepository.delete(member);
    }

    @Override
    @Transactional
    public void quitProject(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId)
                .orElseThrow(() -> new BusinessException("你不是该项目成员"));
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(member.getRole())) {
            throw new BusinessException("项目所有者不能直接退出项目，请先转移所有权或删除项目");
        }
        handleTasksAfterMemberDeparture(projectId, currentUserId);
        projectMemberRepository.delete(member);
    }

    private void validateRole(String role) {
        if (!ProjectMemberRoleEnum.contains(role)) {
            throw new BusinessException("成员角色不合法");
        }
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(role)) {
            throw new BusinessException("不能直接设置项目所有者角色");
        }
    }

    private void assertCanGrantRole(Project project, Long currentUserId, String targetRole) {
        String operatorRole = resolveOperatorRole(project, currentUserId);
        if (!canOperateLowerRole(operatorRole, targetRole)) {
            throw new BusinessException("只能分配权限低于自己的角色");
        }
    }

    private void assertTargetRoleManageable(Project project, Long currentUserId, String targetRole) {
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(targetRole)) {
            throw new BusinessException("项目所有者不可被直接修改或移除");
        }
        String operatorRole = resolveOperatorRole(project, currentUserId);
        if (!canOperateLowerRole(operatorRole, targetRole)) {
            throw new BusinessException("只能管理权限低于自己的成员");
        }
    }

    private String resolveOperatorRole(Project project, Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        if (currentUserId.equals(project.getAuthorId())) {
            return ProjectMemberRoleEnum.OWNER.getValue();
        }
        return projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .map(ProjectMember::getRole)
                .orElseThrow(() -> new BusinessException("当前用户不是项目有效管理成员"));
    }

    private boolean canOperateLowerRole(String operatorRole, String targetRole) {
        return roleWeight(operatorRole) > roleWeight(targetRole);
    }

    private int roleWeight(String role) {
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(role)) {
            return 3;
        }
        if (ProjectMemberRoleEnum.ADMIN.getValue().equals(role)) {
            return 2;
        }
        if (ProjectMemberRoleEnum.MEMBER.getValue().equals(role)) {
            return 1;
        }
        if (ProjectMemberRoleEnum.VIEWER.getValue().equals(role)) {
            return 0;
        }
        return -1;
    }

    private void handleTasksAfterMemberDeparture(Long projectId, Long userId) {
        List<ProjectTask> tasks = projectTaskRepository.findByProjectIdAndAssigneeIdOrderByCreatedAtDesc(projectId, userId);
        if (tasks.isEmpty()) {
            return;
        }
        boolean changed = false;
        LocalDateTime now = LocalDateTime.now();
        for (ProjectTask task : tasks) {
            if (ProjectTaskStatusEnum.DONE.getValue().equals(task.getStatus())) {
                if (task.getCompletedAt() == null) {
                    task.setCompletedAt(task.getUpdatedAt() != null ? task.getUpdatedAt() : now);
                    changed = true;
                }
                continue;
            }
            if (!ProjectTaskStatusEnum.TODO.getValue().equals(task.getStatus())) {
                task.setStatus(ProjectTaskStatusEnum.TODO.getValue());
                changed = true;
            }
            if (task.getCompletedAt() != null) {
                task.setCompletedAt(null);
                changed = true;
            }
            if (task.getAssigneeId() != null) {
                task.setAssigneeId(null);
                changed = true;
            }
        }
        if (changed) {
            projectTaskRepository.saveAll(tasks);
        }
    }

    private Project getProjectOrThrow(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private UserInfoLite getActiveUserOrThrow(Long userId) {
        UserInfoLite user = userInfoLiteRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("用户不存在"));
        if (!isActiveUser(user)) {
            throw new BusinessException("该用户状态不可用，无法加入项目");
        }
        return user;
    }

    private boolean isActiveUser(UserInfoLite user) {
        if (user == null) {
            return false;
        }
        if (!StringUtils.hasText(user.getStatus())) {
            return true;
        }
        return "active".equalsIgnoreCase(user.getStatus().trim());
    }
}
