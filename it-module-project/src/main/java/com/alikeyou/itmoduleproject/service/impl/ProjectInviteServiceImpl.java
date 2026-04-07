
package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectInviteCreateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectInvitation;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectInvitationRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectInviteService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectInviteServiceImpl implements ProjectInviteService {

    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    @Transactional
    public ProjectInvitationVO createInvite(Long projectId, ProjectInviteCreateRequest request, Long currentUserId) {
        Project project = getProject(projectId);
        assertManager(project, currentUserId);
        if (request == null) {
            throw new BusinessException("邀请参数不能为空");
        }
        if (request.getInviteeId() == null && !StringUtils.hasText(request.getInviteeEmail())) {
            throw new BusinessException("请选择被邀请人");
        }
        if (request.getInviteeId() != null && isActiveMember(projectId, request.getInviteeId())) {
            throw new BusinessException("该用户已是项目成员");
        }

        ProjectInvitation saved = projectInvitationRepository.save(ProjectInvitation.builder()
                .projectId(projectId)
                .inviterId(currentUserId)
                .inviteeId(request.getInviteeId())
                .inviteeEmail(trimToNull(request.getInviteeEmail()))
                .inviteRole(normalizeRole(request.getInviteRole(), "member"))
                .inviteCode(UUID.randomUUID().toString().replace("-", ""))
                .inviteMessage(trimToNull(request.getInviteMessage()))
                .status("pending")
                .expiredAt(parseDateTime(request.getExpiredAt(), LocalDateTime.now().plusDays(7)))
                .build());

        projectActivityLogService.record(projectId, currentUserId, "create_invite", "invitation", saved.getId(), "发起项目邀请");
        return toVO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvitationVO> listInvites(Long projectId, Long currentUserId) {
        Project project = getProject(projectId);
        assertManager(project, currentUserId);
        return projectInvitationRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectInvitationVO getInviteDetail(String inviteCode, Long currentUserId) {
        ProjectInvitation invitation = getInvitationByCode(inviteCode);
        Project project = getProject(invitation.getProjectId());
        if (invitation.getInviteeId() != null && !Objects.equals(invitation.getInviteeId(), currentUserId) && !canManage(project, currentUserId)) {
            throw new BusinessException("无权查看该邀请");
        }
        return toVO(invitation);
    }

    @Override
    @Transactional
    public ProjectInvitationVO acceptInvite(String inviteCode, Long currentUserId) {
        ProjectInvitation invitation = getInvitationByCode(inviteCode);
        Project project = getProject(invitation.getProjectId());

        if (!"pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new BusinessException("该邀请已失效或已处理");
        }
        if (invitation.getExpiredAt() != null && invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("expired");
            projectInvitationRepository.save(invitation);
            throw new BusinessException("邀请已过期");
        }
        if (invitation.getInviteeId() != null && !Objects.equals(invitation.getInviteeId(), currentUserId)) {
            throw new BusinessException("该邀请不属于当前用户");
        }

        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId).orElse(null);
        if (member == null) {
            projectMemberRepository.save(ProjectMember.builder()
                    .projectId(project.getId())
                    .userId(currentUserId)
                    .role(normalizeRole(invitation.getInviteRole(), "member"))
                    .status("active")
                    .build());
        } else {
            member.setRole(normalizeRole(invitation.getInviteRole(), member.getRole()));
            member.setStatus("active");
            projectMemberRepository.save(member);
        }

        invitation.setStatus("accepted");
        invitation.setRespondedAt(LocalDateTime.now());
        ProjectInvitation saved = projectInvitationRepository.save(invitation);

        projectActivityLogService.record(project.getId(), currentUserId, "accept_invite", "invitation", saved.getId(), "接受项目邀请");
        projectActivityLogService.record(project.getId(), currentUserId, "add_member", "member", currentUserId, "通过邀请加入项目");
        return toVO(saved);
    }

    @Override
    @Transactional
    public void cancelInvite(Long inviteId, Long currentUserId) {
        ProjectInvitation invitation = projectInvitationRepository.findById(inviteId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));
        Project project = getProject(invitation.getProjectId());
        assertManager(project, currentUserId);
        invitation.setStatus("cancelled");
        invitation.setRespondedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);
        projectActivityLogService.record(project.getId(), currentUserId, "cancel_invite", "invitation", inviteId, "取消项目邀请");
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private ProjectInvitation getInvitationByCode(String inviteCode) {
        if (!StringUtils.hasText(inviteCode)) {
            throw new BusinessException("邀请编码不能为空");
        }
        return projectInvitationRepository.findByInviteCode(inviteCode.trim())
                .orElseThrow(() -> new BusinessException("邀请不存在"));
    }

    private boolean canManage(Project project, Long currentUserId) {
        if (project == null || currentUserId == null) {
            return false;
        }
        if (Objects.equals(project.getAuthorId(), currentUserId)) {
            return true;
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId).orElse(null);
        if (member == null) {
            return false;
        }
        String role = String.valueOf(member.getRole()).toLowerCase(Locale.ROOT);
        String status = String.valueOf(member.getStatus()).toLowerCase(Locale.ROOT);
        return "active".equals(status) && ("owner".equals(role) || "admin".equals(role));
    }

    private void assertManager(Project project, Long currentUserId) {
        if (!canManage(project, currentUserId)) {
            throw new BusinessException("仅项目所有者或管理员可操作");
        }
    }

    private boolean isActiveMember(Long projectId, Long userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        return member != null && "active".equalsIgnoreCase(String.valueOf(member.getStatus()));
    }

    private String normalizeRole(String value, String defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "admin", "member", "viewer" -> normalized;
            default -> defaultValue;
        };
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private LocalDateTime parseDateTime(String value, LocalDateTime defaultValue) {
        if (!StringUtils.hasText(value)) {
            return defaultValue;
        }
        try {
            return LocalDateTime.parse(value.trim().replace(" ", "T"));
        } catch (DateTimeParseException ignored) {
            return defaultValue;
        }
    }

    private ProjectInvitationVO toVO(ProjectInvitation item) {
        Project project = projectRepository.findById(item.getProjectId()).orElse(null);
        UserInfoLite inviter = userInfoLiteRepository.findById(item.getInviterId()).orElse(null);
        UserInfoLite invitee = item.getInviteeId() == null ? null : userInfoLiteRepository.findById(item.getInviteeId()).orElse(null);
        return ProjectInvitationVO.builder()
                .id(item.getId())
                .projectId(item.getProjectId())
                .projectName(project == null ? null : project.getName())
                .inviterId(item.getInviterId())
                .inviterName(resolveName(inviter))
                .inviteeId(item.getInviteeId())
                .inviteeName(resolveName(invitee))
                .inviteeEmail(item.getInviteeEmail())
                .inviteRole(item.getInviteRole())
                .inviteMessage(item.getInviteMessage())
                .inviteCode(item.getInviteCode())
                .status(item.getStatus())
                .expiredAt(item.getExpiredAt())
                .respondedAt(item.getRespondedAt())
                .createdAt(item.getCreatedAt())
                .updatedAt(item.getUpdatedAt())
                .build();
    }

    private String resolveName(UserInfoLite user) {
        if (user == null) {
            return null;
        }
        if (StringUtils.hasText(user.getNickname())) {
            return user.getNickname().trim();
        }
        if (StringUtils.hasText(user.getUsername())) {
            return user.getUsername().trim();
        }
        return "用户" + user.getId();
    }
}
