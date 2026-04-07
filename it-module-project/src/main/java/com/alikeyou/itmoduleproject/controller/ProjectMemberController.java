
package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.entity.ProjectInvitation;
import com.alikeyou.itmoduleproject.entity.ProjectJoinRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.repository.ProjectInvitationRepository;
import com.alikeyou.itmoduleproject.repository.ProjectJoinRequestRepository;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectMemberStatusVO;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Locale;
import java.util.Objects;

@RestController
@RequestMapping("/api/project/member")
@RequiredArgsConstructor
@Tag(name = "项目成员模块")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectActivityLogService projectActivityLogService;
    private final ProjectMemberRepository projectMemberRepository;
    private final ProjectRepository projectRepository;
    private final ProjectInvitationRepository projectInvitationRepository;
    private final ProjectJoinRequestRepository projectJoinRequestRepository;

    @GetMapping("/list")
    @Operation(summary = "成员列表")
    public ResponseEntity<ApiResponse<List<ProjectMemberVO>>> listMembers(@RequestParam Long projectId,
                                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.listMembers(projectId, currentUserId)));
    }

    @PostMapping
    @Operation(summary = "添加成员")
    public ResponseEntity<ApiResponse<ProjectMemberVO>> addMember(@RequestBody ProjectMemberAddRequest requestBody,
                                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectMemberVO result = projectMemberService.addMember(requestBody, currentUserId);
        projectActivityLogService.record(requestBody.getProjectId(), currentUserId, "add_member", "member", result == null ? null : result.getId(), "新增成员");
        return ResponseEntity.ok(ApiResponse.ok(result));
    }

    @PutMapping("/role")
    @Operation(summary = "修改成员角色")
    public ResponseEntity<ApiResponse<ProjectMemberVO>> updateRole(@RequestBody ProjectMemberRoleUpdateRequest requestBody,
                                                                   HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.updateMemberRole(requestBody, currentUserId)));
    }

    @DeleteMapping("/{memberId}")
    @Operation(summary = "移除成员")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long memberId,
                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        ProjectMember member = projectMemberRepository.findById(memberId).orElse(null);
        Long projectId = member == null ? null : member.getProjectId();
        projectMemberService.removeMember(memberId, currentUserId);
        if (projectId != null) {
            projectActivityLogService.record(projectId, currentUserId, "remove_member", "member", memberId, "移除成员");
        }
        return ResponseEntity.ok(ApiResponse.ok("移除成功", null));
    }

    @PostMapping("/quit")
    @Operation(summary = "退出项目")
    public ResponseEntity<ApiResponse<Void>> quitProject(@RequestParam Long projectId,
                                                         HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectMemberService.quitProject(projectId, currentUserId);
        projectActivityLogService.record(projectId, currentUserId, "quit_project", "member", currentUserId, "退出项目");
        return ResponseEntity.ok(ApiResponse.ok("退出成功", null));
    }

    @GetMapping("/status")
    @Operation(summary = "当前用户项目成员状态")
    public ResponseEntity<ApiResponse<ProjectMemberStatusVO>> getMemberStatus(@RequestParam Long projectId,
                                                                              HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        Project project = projectRepository.findById(projectId).orElse(null);
        if (project == null) {
            return ResponseEntity.ok(ApiResponse.ok(ProjectMemberStatusVO.builder()
                    .projectId(projectId)
                    .member(false)
                    .canApplyJoin(false)
                    .hasPendingInvite(false)
                    .hasPendingJoinRequest(false)
                    .canInviteOthers(false)
                    .canAuditJoinRequests(false)
                    .build()));
        }

        boolean isOwner = currentUserId != null && Objects.equals(project.getAuthorId(), currentUserId);
        ProjectMember member = currentUserId == null ? null : projectMemberRepository.findByProjectIdAndUserId(projectId, currentUserId).orElse(null);
        boolean isMember = isOwner || (member != null && "active".equalsIgnoreCase(String.valueOf(member.getStatus())));
        String role = isOwner ? "owner" : (member == null ? "" : String.valueOf(member.getRole()).toLowerCase(Locale.ROOT));
        boolean canManage = "owner".equals(role) || "admin".equals(role);

        ProjectJoinRequest joinRequest = currentUserId == null ? null
                : projectJoinRequestRepository.findFirstByProjectIdAndApplicantIdOrderByCreatedAtDesc(projectId, currentUserId).orElse(null);
        boolean hasPendingJoinRequest = joinRequest != null && "pending".equalsIgnoreCase(String.valueOf(joinRequest.getStatus()));

        ProjectInvitation invitation = currentUserId == null ? null
                : projectInvitationRepository.findFirstByProjectIdAndInviteeIdAndStatusOrderByCreatedAtDesc(projectId, currentUserId, "pending").orElse(null);
        boolean hasPendingInvite = invitation != null;

        ProjectMemberStatusVO result = ProjectMemberStatusVO.builder()
                .projectId(projectId)
                .member(isMember)
                .role(role)
                .canApplyJoin(currentUserId != null && !isMember)
                .hasPendingJoinRequest(hasPendingJoinRequest)
                .pendingJoinRequestId(joinRequest == null ? null : joinRequest.getId())
                .hasPendingInvite(hasPendingInvite)
                .pendingInviteId(invitation == null ? null : invitation.getId())
                .pendingInviteCode(invitation == null ? null : invitation.getInviteCode())
                .canInviteOthers(canManage)
                .canAuditJoinRequests(canManage)
                .build();

        return ResponseEntity.ok(ApiResponse.ok(result));
    }
}
