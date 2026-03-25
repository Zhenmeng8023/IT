package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.enums.ProjectMemberRoleEnum;
import com.alikeyou.itmoduleproject.enums.ProjectMemberStatusEnum;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.support.ProjectPermissionService;
import com.alikeyou.itmoduleproject.support.ProjectVoMapper;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {

    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectPermissionService projectPermissionService;

    @Override
    public List<ProjectMemberVO> listMembers(Long projectId, Long currentUserId) {
        projectPermissionService.assertProjectReadable(projectId, currentUserId);
        return projectMemberRepository.findByProjectIdOrderByJoinedAtAsc(projectId)
            .stream()
            .map(ProjectVoMapper::toProjectMemberVO)
            .toList();
    }

    @Override
    @Transactional
    public ProjectMemberVO addMember(ProjectMemberAddRequest request, Long currentUserId) {
        projectPermissionService.assertProjectManageMembers(request.getProjectId(), currentUserId);
        if (!ProjectMemberRoleEnum.contains(request.getRole())) {
            throw new BusinessException("成员角色不合法");
        }
        if (projectMemberRepository.existsByProjectIdAndUserId(request.getProjectId(), request.getUserId())) {
            throw new BusinessException("该用户已在项目成员中");
        }
        projectRepository.findById(request.getProjectId()).orElseThrow(() -> new BusinessException("项目不存在"));
        ProjectMember saved = projectMemberRepository.save(ProjectMember.builder()
            .projectId(request.getProjectId())
            .userId(request.getUserId())
            .role(request.getRole())
            .status(ProjectMemberStatusEnum.ACTIVE.getValue())
            .build());
        return ProjectVoMapper.toProjectMemberVO(saved);
    }

    @Override
    @Transactional
    public ProjectMemberVO updateMemberRole(ProjectMemberRoleUpdateRequest request, Long currentUserId) {
        if (!ProjectMemberRoleEnum.contains(request.getRole())) {
            throw new BusinessException("成员角色不合法");
        }
        ProjectMember member = projectMemberRepository.findById(request.getMemberId())
            .orElseThrow(() -> new BusinessException("项目成员不存在"));
        projectPermissionService.assertProjectManageMembers(member.getProjectId(), currentUserId);
        if ("owner".equals(member.getRole())) {
            throw new BusinessException("项目所有者角色不可修改");
        }
        member.setRole(request.getRole());
        return ProjectVoMapper.toProjectMemberVO(projectMemberRepository.save(member));
    }

    @Override
    @Transactional
    public void removeMember(Long memberId, Long currentUserId) {
        ProjectMember member = projectMemberRepository.findById(memberId)
            .orElseThrow(() -> new BusinessException("项目成员不存在"));
        projectPermissionService.assertProjectManageMembers(member.getProjectId(), currentUserId);
        if ("owner".equals(member.getRole())) {
            throw new BusinessException("项目所有者不可移除");
        }
        projectMemberRepository.delete(member);
    }
}
