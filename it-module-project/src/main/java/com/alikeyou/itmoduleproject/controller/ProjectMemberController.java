package com.alikeyou.itmoduleproject.controller;

import com.alikeyou.itmoduleproject.dto.ProjectMemberAddRequest;
import com.alikeyou.itmoduleproject.dto.ProjectMemberRoleUpdateRequest;
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
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.addMember(requestBody, currentUserId)));
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
        projectMemberService.removeMember(memberId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("移除成功", null));
    }
}
