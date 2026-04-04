package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
import com.alikeyou.itmoduleproject.entity.ProjectMember;
import com.alikeyou.itmoduleproject.repository.ProjectMemberRepository;
import com.alikeyou.itmoduleproject.service.ProjectActivityLogService;
import com.alikeyou.itmoduleproject.service.ProjectMemberService;
import com.alikeyou.itmoduleproject.support.CurrentUserProvider;
import com.alikeyou.itmoduleproject.vo.ApiResponse;
import com.alikeyou.itmoduleproject.vo.ProjectMemberVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/project/member")
@RequiredArgsConstructor
@Tag(name = "项目成员模块")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;
    private final CurrentUserProvider currentUserProvider;
    private final ProjectActivityLogService projectActivityLogService;
    private final ProjectMemberRepository projectMemberRepository;

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
}
