
package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectInviteAcceptRequest;
import com.alikeyou.itmoduleproject.dto.ProjectInviteCreateRequest;
import com.alikeyou.itmoduleproject.service.ProjectInviteService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectInvitationVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project")
@RequiredArgsConstructor
@Tag(name = "项目邀请")
public class ProjectInviteController {

    private final ProjectInviteService projectInviteService;
    private final CurrentUserProvider currentUserProvider;

    @PostMapping("/{projectId}/invites")
    @Operation(summary = "创建项目邀请")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> createInvite(
            @PathVariable Long projectId,
            @RequestBody ProjectInviteCreateRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInviteService.createInvite(projectId, requestBody, currentUserId)));
    }

    @GetMapping("/{projectId}/invites")
    @Operation(summary = "邀请列表")
    public ResponseEntity<ApiResponse<List<ProjectInvitationVO>>> listInvites(
            @PathVariable Long projectId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInviteService.listInvites(projectId, currentUserId)));
    }

    @GetMapping("/invites/{inviteCode}")
    @Operation(summary = "邀请详情")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> getInviteDetail(
            @PathVariable String inviteCode,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdOrNull(request);
        return ResponseEntity.ok(ApiResponse.ok(projectInviteService.getInviteDetail(inviteCode, currentUserId)));
    }

    @PostMapping("/invites/{inviteCode}/accept")
    @Operation(summary = "接受邀请")
    public ResponseEntity<ApiResponse<ProjectInvitationVO>> acceptInvite(
            @PathVariable String inviteCode,
            @RequestBody(required = false) ProjectInviteAcceptRequest requestBody,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        String code = requestBody != null && requestBody.getInviteCode() != null ? requestBody.getInviteCode() : inviteCode;
        return ResponseEntity.ok(ApiResponse.ok(projectInviteService.acceptInvite(code, currentUserId)));
    }

    @PostMapping("/invites/{inviteId}/cancel")
    @Operation(summary = "取消邀请")
    public ResponseEntity<ApiResponse<Void>> cancelInvite(
            @PathVariable Long inviteId,
            HttpServletRequest request
    ) {
        Long currentUserId = currentUserProvider.getCurrentUserIdRequired(request);
        projectInviteService.cancelInvite(inviteId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("取消成功", null));
    }
}
