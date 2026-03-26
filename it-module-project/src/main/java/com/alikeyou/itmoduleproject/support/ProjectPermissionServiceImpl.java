package com.alikeyou.itmoduleproject.support;

import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.enums.ProjectVisibilityEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectPermissionServiceImpl implements ProjectPermissionService {

    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;

    @Override
    public void assertProjectReadable(Long projectId, Long userId) {
        Project project = getProject(projectId);

        if (ProjectVisibilityEnum.PUBLIC.getValue().equals(project.getVisibility())) {
            return;
        }

        if (userId == null) {
            throw new BusinessException("无权查看该项目");
        }

        if (userId.equals(project.getAuthorId())) {
            return;
        }

        projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .orElseThrow(() -> new BusinessException("无权查看该项目"));
    }

    @Override
    public void assertProjectWritable(Long projectId, Long userId) {
        if (userId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        if (canManageProject(projectId, userId)) {
            return;
        }
        throw new BusinessException("无权操作该项目");
    }

    @Override
    public void assertProjectManageMembers(Long projectId, Long userId) {
        if (userId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        if (canManageProject(projectId, userId)) {
            return;
        }
        throw new BusinessException("无权管理项目成员");
    }

    @Override
    public void assertProjectOwner(Long projectId, Long userId) {
        if (userId == null) {
            throw new BusinessException("当前请求未登录或登录信息已失效");
        }
        Project project = getProject(projectId);
        if (!userId.equals(project.getAuthorId())) {
            throw new BusinessException("仅项目所有者可执行此操作");
        }
    }

    @Override
    public boolean canManageProject(Long projectId, Long userId) {
        if (userId == null) {
            return false;
        }
        Project project = getProject(projectId);
        if (userId.equals(project.getAuthorId())) {
            return true;
        }
        return projectMemberRepository.findByProjectIdAndUserId(projectId, userId)
                .filter(member -> ProjectMemberStatusEnum.ACTIVE.getValue().equals(member.getStatus()))
                .map(ProjectMember::getRole)
                .map(role -> "owner".equals(role) || "admin".equals(role))
                .orElse(false);
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }
}
