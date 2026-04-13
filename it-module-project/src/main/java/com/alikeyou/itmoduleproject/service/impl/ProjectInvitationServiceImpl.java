package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublisher;
import com.alikeyou.itmoduleproject.dto.ProjectInvitationCreateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectInvitation;
import com.alikeyou.itmoduleproject.entity.ProjectJoinRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectInvitationRepository;
import com.alikeyou.itmoduleproject.repository.ProjectJoinRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectInvitationService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProjectInvitationServiceImpl implements ProjectInvitationService {

    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectJoinRequestRepository projectJoinRequestRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectActivityLogService projectActivityLogService;
    private final NotificationPublisher notificationPublisher;

    @Override
    @Transactional
    public ProjectInvitationVO createInvitation(ProjectInvitationCreateRequest request, Long currentUserId) {
        if (request == null || request.getProjectId() == null) {
            throw new BusinessException("项目ID不能为空");
        }
        if (request.getInviteeId() == null) {
            throw new BusinessException("请选择要邀请的用户");
        }
        Project project = getProject(request.getProjectId());
        assertCanInvite(project, currentUserId);
        getActiveUser(request.getInviteeId());
        assertInviteTargetValid(project, request.getInviteeId(), currentUserId);
        if (projectInvitationRepository.existsByProjectIdAndInviteeIdAndStatus(project.getId(), request.getInviteeId(), "pending")) {
            throw new BusinessException("该用户已有待处理邀请");
        }

        ProjectInvitation saved = projectInvitationRepository.save(ProjectInvitation.builder()
                .projectId(project.getId())
                .inviterId(currentUserId)
                .inviteeId(request.getInviteeId())
                .inviteeEmail(null)
                .inviteRole(normalizeInviteRole(request.getInviteRole()))
                .inviteCode(UUID.randomUUID().toString().replace("-", ""))
                .inviteMessage(trimToNull(request.getInviteMessage()))
                .status("pending")
                .expiredAt(resolveExpiredAt(request.getExpireDays()))
                .build());
        projectActivityLogService.record(project.getId(), currentUserId, "invite_member", "invitation", saved.getId(), "发送项目邀请");
        publishInvitationCreatedNotification(saved, project);
        return toVO(saved, project);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvitationVO> listProjectInvitations(Long projectId, String status, Long currentUserId) {
        Project project = getProject(projectId);
        assertCanInvite(project, currentUserId);
        List<ProjectInvitation> list = StringUtils.hasText(status)
                ? projectInvitationRepository.findByProjectIdAndStatusOrderByCreatedAtDesc(projectId, status.trim().toLowerCase(Locale.ROOT))
                : projectInvitationRepository.findByProjectIdOrderByCreatedAtDesc(projectId);
        return list.stream().map(item -> toVO(item, project)).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectInvitationVO> listMyPendingInvitations(Long currentUserId) {
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        return projectInvitationRepository.findByInviteeIdAndStatusOrderByCreatedAtDesc(currentUserId, "pending")
                .stream()
                .filter(item -> item.getExpiredAt() == null || item.getExpiredAt().isAfter(LocalDateTime.now()))
                .map(item -> toVO(item, getProject(item.getProjectId())))
                .toList();
    }

    @Override
    @Transactional
    public ProjectInvitationVO acceptInvitation(Long invitationId, Long currentUserId) {
        ProjectInvitation invitation = projectInvitationRepository.findByIdAndInviteeId(invitationId, currentUserId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));
        if (!"pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new BusinessException("该邀请已处理");
        }
        if (invitation.getExpiredAt() != null && invitation.getExpiredAt().isBefore(LocalDateTime.now())) {
            invitation.setStatus("expired");
            projectInvitationRepository.save(invitation);
            throw new BusinessException("邀请已过期");
        }
        Project project = getProject(invitation.getProjectId());
        if (Objects.equals(project.getAuthorId(), currentUserId)) {
            throw new BusinessException("你已是该项目所有者");
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId).orElse(null);
        if (member == null) {
            projectMemberRepository.save(ProjectMember.builder()
                    .projectId(project.getId())
                    .userId(currentUserId)
                    .role(normalizeInviteRole(invitation.getInviteRole()))
                    .status("active")
                    .build());
        } else {
            member.setRole(normalizeInviteRole(invitation.getInviteRole()));
            member.setStatus("active");
            projectMemberRepository.save(member);
        }
        invitation.setStatus("accepted");
        invitation.setRespondedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);

        ProjectJoinRequest joinRequest = projectJoinRequestRepository.findFirstByProjectIdAndApplicantIdOrderByCreatedAtDesc(project.getId(), currentUserId).orElse(null);
        if (joinRequest != null && "pending".equalsIgnoreCase(joinRequest.getStatus())) {
            joinRequest.setStatus("approved");
            joinRequest.setReviewedAt(LocalDateTime.now());
            joinRequest.setReviewMessage("已通过邀请加入项目");
            projectJoinRequestRepository.save(joinRequest);
        }
        projectActivityLogService.record(project.getId(), currentUserId, "accept_invitation", "invitation", invitation.getId(), "接受项目邀请");
        projectActivityLogService.record(project.getId(), currentUserId, "add_member", "member", currentUserId, "通过邀请加入项目");
        notificationPublisher.updateBusinessStatus("project_invitation", invitation.getId(), "handled");
        publishInvitationResponseNotification(invitation, project, currentUserId, "accepted");
        return toVO(invitation, project);
    }

    @Override
    @Transactional
    public ProjectInvitationVO rejectInvitation(Long invitationId, Long currentUserId) {
        ProjectInvitation invitation = projectInvitationRepository.findByIdAndInviteeId(invitationId, currentUserId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));
        if (!"pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new BusinessException("该邀请已处理");
        }
        invitation.setStatus("rejected");
        invitation.setRespondedAt(LocalDateTime.now());
        ProjectInvitation saved = projectInvitationRepository.save(invitation);
        projectActivityLogService.record(invitation.getProjectId(), currentUserId, "reject_invitation", "invitation", invitation.getId(), "拒绝项目邀请");
        Project project = getProject(invitation.getProjectId());
        notificationPublisher.updateBusinessStatus("project_invitation", invitation.getId(), "handled");
        publishInvitationResponseNotification(saved, project, currentUserId, "rejected");
        return toVO(saved, project);
    }

    @Override
    @Transactional
    public void cancelInvitation(Long invitationId, Long currentUserId) {
        ProjectInvitation invitation = projectInvitationRepository.findById(invitationId)
                .orElseThrow(() -> new BusinessException("邀请不存在"));
        Project project = getProject(invitation.getProjectId());
        assertCanInvite(project, currentUserId);
        if (!"pending".equalsIgnoreCase(invitation.getStatus())) {
            throw new BusinessException("该邀请已处理");
        }
        invitation.setStatus("cancelled");
        invitation.setRespondedAt(LocalDateTime.now());
        projectInvitationRepository.save(invitation);
        projectActivityLogService.record(project.getId(), currentUserId, "cancel_invitation", "invitation", invitation.getId(), "撤销项目邀请");
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId).orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private void assertCanInvite(Project project, Long currentUserId) {
        if (project == null || currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        if (Objects.equals(project.getAuthorId(), currentUserId)) {
            return;
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), currentUserId).orElse(null);
        if (member == null || !"active".equalsIgnoreCase(String.valueOf(member.getStatus()))) {
            throw new BusinessException("仅项目所有者或管理员可发送邀请");
        }
        String role = String.valueOf(member.getRole()).toLowerCase(Locale.ROOT);
        if (!"owner".equals(role) && !"admin".equals(role)) {
            throw new BusinessException("仅项目所有者或管理员可发送邀请");
        }
    }

    private void assertInviteTargetValid(Project project, Long inviteeId, Long currentUserId) {
        if (Objects.equals(inviteeId, currentUserId)) {
            throw new BusinessException("不能邀请自己加入项目");
        }
        if (Objects.equals(project.getAuthorId(), inviteeId)) {
            throw new BusinessException("该用户已是项目所有者");
        }
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), inviteeId).orElse(null);
        if (member != null && "active".equalsIgnoreCase(String.valueOf(member.getStatus()))) {
            throw new BusinessException("该用户已在项目成员中");
        }
    }

    private UserInfoLite getActiveUser(Long userId) {
        UserInfoLite user = userInfoLiteRepository.findById(userId).orElseThrow(() -> new BusinessException("用户不存在"));
        if (StringUtils.hasText(user.getStatus()) && !"active".equalsIgnoreCase(user.getStatus().trim())) {
            throw new BusinessException("该用户状态不可用");
        }
        return user;
    }

    private String normalizeInviteRole(String value) {
        String role = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "member";
        if ("admin".equals(role) || "viewer".equals(role) || "member".equals(role)) {
            return role;
        }
        return "member";
    }

    private LocalDateTime resolveExpiredAt(Integer expireDays) {
        int days = expireDays == null || expireDays <= 0 ? 7 : expireDays;
        return LocalDateTime.now().plusDays(days);
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
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

    private String resolveSummary(Project project) {
        if (project == null || !StringUtils.hasText(project.getDescription())) {
            return null;
        }
        String value = project.getDescription().trim();
        return value.length() > 100 ? value.substring(0, 100) + "..." : value;
    }

    private void publishInvitationCreatedNotification(ProjectInvitation invitation, Project project) {
        if (invitation == null || invitation.getInviteeId() == null || Objects.equals(invitation.getInviteeId(), invitation.getInviterId())) {
            return;
        }
        notificationPublisher.publish(NotificationCreateCommand.builder()
                .receiverId(invitation.getInviteeId())
                .senderId(invitation.getInviterId())
                .category("invite")
                .type("project_invitation")
                .title("项目邀请")
                .content("邀请你加入项目《" + safeProjectName(project) + "》")
                .targetType("invitation")
                .targetId(invitation.getId())
                .sourceType("project_invitation")
                .sourceId(invitation.getId())
                .eventKey("project_invitation:" + invitation.getId() + ":created")
                .actionUrl("/myproject?tab=invitations&projectId=" + invitation.getProjectId() + "&invitationId=" + invitation.getId())
                .businessStatus("open")
                .priority(5)
                .payload(projectPayload(project, invitation.getId(), null))
                .build());
    }

    private void publishInvitationResponseNotification(ProjectInvitation invitation, Project project, Long actorId, String status) {
        if (invitation == null || invitation.getInviterId() == null || actorId == null || Objects.equals(invitation.getInviterId(), actorId)) {
            return;
        }
        boolean accepted = "accepted".equalsIgnoreCase(status);
        String actorName = userInfoLiteRepository.findById(actorId).map(this::resolveName).orElse("对方");
        notificationPublisher.publish(NotificationCreateCommand.builder()
                .receiverId(invitation.getInviterId())
                .senderId(actorId)
                .category("invite")
                .type(accepted ? "project_invitation_accepted" : "project_invitation_rejected")
                .title(accepted ? "邀请已接受" : "邀请已拒绝")
                .content(actorName + (accepted ? " 接受了" : " 拒绝了") + "项目《" + safeProjectName(project) + "》的邀请")
                .targetType("project")
                .targetId(invitation.getProjectId())
                .sourceType("project_invitation_response")
                .sourceId(invitation.getId())
                .eventKey("project_invitation:" + invitation.getId() + ":" + status)
                .actionUrl("/projectdetail?projectId=" + invitation.getProjectId())
                .businessStatus("handled")
                .priority(3)
                .payload(projectPayload(project, invitation.getId(), status))
                .build());
    }

    private Map<String, Object> projectPayload(Project project, Long invitationId, String status) {
        Map<String, Object> payload = new LinkedHashMap<>();
        if (project != null) {
            payload.put("projectId", project.getId());
            payload.put("projectName", project.getName());
            payload.put("targetTitle", project.getName());
        }
        if (invitationId != null) {
            payload.put("invitationId", invitationId);
        }
        if (status != null) {
            payload.put("status", status);
        }
        return payload;
    }

    private String safeProjectName(Project project) {
        if (project == null || !StringUtils.hasText(project.getName())) {
            return "相关项目";
        }
        return project.getName();
    }

    private ProjectInvitationVO toVO(ProjectInvitation invitation, Project project) {
        UserInfoLite inviter = invitation.getInviterId() == null ? null : userInfoLiteRepository.findById(invitation.getInviterId()).orElse(null);
        UserInfoLite invitee = invitation.getInviteeId() == null ? null : userInfoLiteRepository.findById(invitation.getInviteeId()).orElse(null);
        return ProjectInvitationVO.builder()
                .id(invitation.getId())
                .projectId(invitation.getProjectId())
                .projectName(project == null ? null : project.getName())
                .projectDescription(project == null ? null : project.getDescription())
                .projectCategory(project == null ? null : project.getCategory())
                .projectVisibility(project == null ? null : project.getVisibility())
                .projectStatus(project == null ? null : project.getStatus())
                .projectCover(null)
                .projectSummary(resolveSummary(project))
                .inviterId(invitation.getInviterId())
                .inviterName(resolveName(inviter))
                .inviterAvatar(inviter == null ? null : inviter.getAvatarUrl())
                .inviteeId(invitation.getInviteeId())
                .inviteeName(resolveName(invitee))
                .inviteeEmail(invitation.getInviteeEmail())
                .inviteRole(invitation.getInviteRole())
                .inviteMessage(invitation.getInviteMessage())
                .inviteCode(invitation.getInviteCode())
                .status(invitation.getStatus())
                .expiredAt(invitation.getExpiredAt())
                .respondedAt(invitation.getRespondedAt())
                .createdAt(invitation.getCreatedAt())
                .updatedAt(invitation.getUpdatedAt())
                .build();
    }
}
