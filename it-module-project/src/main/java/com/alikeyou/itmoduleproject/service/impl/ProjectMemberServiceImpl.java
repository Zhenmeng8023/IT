package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
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

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
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
        if (!ProjectMemberRoleEnum.contains(request.getRole())) {
            throw new BusinessException("成员角色不合法");
        }
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(request.getRole())) {
            throw new BusinessException("不能直接添加项目所有者角色");
        }
        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new BusinessException("项目不存在"));
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
        if (!ProjectMemberRoleEnum.contains(request.getRole())) {
            throw new BusinessException("成员角色不合法");
        }
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(request.getRole())) {
            throw new BusinessException("项目所有者角色不可直接设置");
        }
        ProjectMember member = projectMemberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new BusinessException("项目成员不存在"));
        projectPermissionService.assertProjectManageMembers(member.getProjectId(), currentUserId);
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(member.getRole())) {
            throw new BusinessException("项目所有者角色不可修改");
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
        if (ProjectMemberRoleEnum.OWNER.getValue().equals(member.getRole())) {
            throw new BusinessException("项目所有者不可移除");
        }
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
        projectMemberRepository.delete(member);
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
