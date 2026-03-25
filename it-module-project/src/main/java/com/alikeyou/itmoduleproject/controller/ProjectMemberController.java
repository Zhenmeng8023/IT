/**
 * 项目成员控制器
 * 处理项目成员相关的HTTP请求
 */
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

/**
 * 项目成员控制器
 * 处理项目成员相关的HTTP请求
 */
@RestController
@RequestMapping("/api/project/member")
@RequiredArgsConstructor
@Tag(name = "项目成员模块")
public class ProjectMemberController {

    /**
     * 项目成员服务
     */
    private final ProjectMemberService projectMemberService;
    /**
     * 当前用户提供者
     */
    private final CurrentUserProvider currentUserProvider;

    /**
     * 获取项目成员列表
     * @param projectId 项目ID
     * @param request HTTP请求对象
     * @return 成员列表响应
     */
    @GetMapping("/list")
    @Operation(summary = "成员列表")
    public ResponseEntity<ApiResponse<List<ProjectMemberVO>>> listMembers(@RequestParam Long projectId,
                                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.listMembers(projectId, currentUserId)));
    }

    /**
     * 添加项目成员
     * @param requestBody 添加成员请求参数
     * @param request HTTP请求对象
     * @return 成员信息响应
     */
    @PostMapping
    @Operation(summary = "添加成员")
    public ResponseEntity<ApiResponse<ProjectMemberVO>> addMember(@RequestBody ProjectMemberAddRequest requestBody,
                                                                  HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.addMember(requestBody, currentUserId)));
    }

    /**
     * 修改成员角色
     * @param requestBody 修改角色请求参数
     * @param request HTTP请求对象
     * @return 成员信息响应
     */
    @PutMapping("/role")
    @Operation(summary = "修改成员角色")
    public ResponseEntity<ApiResponse<ProjectMemberVO>> updateRole(@RequestBody ProjectMemberRoleUpdateRequest requestBody,
                                                                   HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        return ResponseEntity.ok(ApiResponse.ok(projectMemberService.updateMemberRole(requestBody, currentUserId)));
    }

    /**
     * 移除项目成员
     * @param memberId 成员ID
     * @param request HTTP请求对象
     * @return 移除成功响应
     */
    @DeleteMapping("/{memberId}")
    @Operation(summary = "移除成员")
    public ResponseEntity<ApiResponse<Void>> removeMember(@PathVariable Long memberId,
                                                          HttpServletRequest request) {
        Long currentUserId = currentUserProvider.getCurrentUserId(request);
        projectMemberService.removeMember(memberId, currentUserId);
        return ResponseEntity.ok(ApiResponse.ok("移除成功", null));
    }
}