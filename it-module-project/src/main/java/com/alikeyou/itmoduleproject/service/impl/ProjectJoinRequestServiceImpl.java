
package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestAuditRequest;
import com.alikeyou.itmoduleproject.dto.ProjectJoinRequestCreateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectJoinRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.entity.UserInfoLite;
import com.alikeyou.itmoduleproject.repository.ProjectJoinRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.repository.UserInfoLiteRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectJoinRequestService;
import com.alikeyou.itmoduleproject.support.BusinessException;
import com.alikeyou.itmoduleproject.vo.ProjectJoinRequestVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

@Service
@RequiredArgsConstructor
public class ProjectJoinRequestServiceImpl implements ProjectJoinRequestService {

    private final ProjectJoinRequestRepository projectJoinRequestRepository;
    private final ProjectRepository projectRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final UserInfoLiteRepository userInfoLiteRepository;
    private final ProjectActivityLogService projectActivityLogService;

    @Override
    @Transactional
    public ProjectJoinRequestVO submitJoinRequest(Long projectId, ProjectJoinRequestCreateRequest request, Long currentUserId) {
        Project project = getProject(projectId);
        if (currentUserId == null) {
            throw new BusinessException("请先登录");
        }
        if (Objects.equals(project.getAuthorId(), currentUserId) || isActiveMember(projectId, currentUserId)) {
            throw new BusinessException("你已加入该项目");
        }
        if (projectJoinRequestRepository.existsByProjectIdAndApplicantIdAndStatus(projectId, currentUserId, "pending")) {
            throw new BusinessException("你已有待处理的加入申请");
        }
        ProjectJoinRequest saved = projectJoinRequestRepository.save(ProjectJoinRequest.builder()
                .projectId(projectId)
                .applicantId(currentUserId)
                .desiredRole(normalizeDesiredRole(request == null ? null : request.getDesiredRole()))
                .applyMessage(trimToNull(request == null ? null : request.getApplyMessage()))
                .status("pending")
                .build());
        projectActivityLogService.record(projectId, currentUserId, "submit_join_request", "join_request", saved.getId(), "提交加入申请");
        return toVO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProjectJoinRequestVO> listJoinRequests(Long projectId, Long currentUserId) {
        Project project = getProject(projectId);
        assertManager(project, currentUserId);
        return projectJoinRequestRepository.findByProjectIdOrderByCreatedAtDesc(projectId).stream()
                .map(this::toVO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public ProjectJoinRequestVO getMyJoinRequestStatus(Long projectId, Long currentUserId) {
        if (currentUserId == null) {
            return null;
        }
        return projectJoinRequestRepository.findFirstByProjectIdAndApplicantIdOrderByCreatedAtDesc(projectId, currentUserId)
                .map(this::toVO)
                .orElse(null);
    }

    @Override
    @Transactional
    public ProjectJoinRequestVO auditJoinRequest(Long requestId, ProjectJoinRequestAuditRequest request, Long currentUserId) {
        ProjectJoinRequest joinRequest = projectJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("加入申请不存在"));
        Project project = getProject(joinRequest.getProjectId());
        assertManager(project, currentUserId);
        if (!"pending".equalsIgnoreCase(joinRequest.getStatus())) {
            throw new BusinessException("该申请已处理");
        }

        String auditStatus = StringUtils.hasText(request == null ? null : request.getStatus())
                ? request.getStatus().trim().toLowerCase(Locale.ROOT)
                : "rejected";
        if ("approved".equals(auditStatus)) {
            ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(project.getId(), joinRequest.getApplicantId()).orElse(null);
            if (member == null) {
                projectMemberRepository.save(ProjectMember.builder()
                        .projectId(project.getId())
                        .userId(joinRequest.getApplicantId())
                        .role(normalizeDesiredRole(joinRequest.getDesiredRole()))
                        .status("active")
                        .build());
            } else {
                member.setRole(normalizeDesiredRole(joinRequest.getDesiredRole()));
                member.setStatus("active");
                projectMemberRepository.save(member);
            }
            joinRequest.setStatus("approved");
            projectActivityLogService.record(project.getId(), currentUserId, "approve_join_request", "join_request", joinRequest.getId(), "通过加入申请");
            projectActivityLogService.record(project.getId(), currentUserId, "add_member", "member", joinRequest.getApplicantId(), "审批通过并加入项目");
        } else {
            joinRequest.setStatus("rejected");
            projectActivityLogService.record(project.getId(), currentUserId, "reject_join_request", "join_request", joinRequest.getId(), "拒绝加入申请");
        }
        joinRequest.setReviewerId(currentUserId);
        joinRequest.setReviewMessage(trimToNull(request == null ? null : request.getReviewMessage()));
        joinRequest.setReviewedAt(LocalDateTime.now());
        return toVO(projectJoinRequestRepository.save(joinRequest));
    }

    @Override
    @Transactional
    public void cancelJoinRequest(Long requestId, Long currentUserId) {
        ProjectJoinRequest joinRequest = projectJoinRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException("加入申请不存在"));
        if (!Objects.equals(joinRequest.getApplicantId(), currentUserId)) {
            throw new BusinessException("仅申请人可取消加入申请");
        }
        if (!"pending".equalsIgnoreCase(joinRequest.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        joinRequest.setStatus("cancelled");
        joinRequest.setReviewedAt(LocalDateTime.now());
        projectJoinRequestRepository.save(joinRequest);
    }

    private Project getProject(Long projectId) {
        return projectRepository.findById(projectId)
                .orElseThrow(() -> new BusinessException("项目不存在"));
    }

    private boolean isActiveMember(Long projectId, Long userId) {
        ProjectMember member = projectMemberRepository.findByProjectIdAndUserId(projectId, userId).orElse(null);
        return member != null && "active".equalsIgnoreCase(String.valueOf(member.getStatus()));
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

    private String normalizeDesiredRole(String value) {
        String normalized = StringUtils.hasText(value) ? value.trim().toLowerCase(Locale.ROOT) : "member";
        return "viewer".equals(normalized) ? "viewer" : "member";
    }

    private String trimToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private ProjectJoinRequestVO toVO(ProjectJoinRequest item) {
        Project project = projectRepository.findById(item.getProjectId()).orElse(null);
        UserInfoLite applicant = userInfoLiteRepository.findById(item.getApplicantId()).orElse(null);
        UserInfoLite reviewer = item.getReviewerId() == null ? null : userInfoLiteRepository.findById(item.getReviewerId()).orElse(null);
        return ProjectJoinRequestVO.builder()
                .id(item.getId())
                .projectId(item.getProjectId())
                .projectName(project == null ? null : project.getName())
                .applicantId(item.getApplicantId())
                .applicantName(resolveName(applicant))
                .desiredRole(item.getDesiredRole())
                .applyMessage(item.getApplyMessage())
                .status(item.getStatus())
                .reviewerId(item.getReviewerId())
                .reviewerName(resolveName(reviewer))
                .reviewMessage(item.getReviewMessage())
                .reviewedAt(item.getReviewedAt())
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
