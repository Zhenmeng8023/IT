package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/api/circle")
@Tag(name = "圈子成员管理", description = "圈子成员的加入、退出、角色管理等接口")
public class CircleMemberController {

    @Autowired
    private CircleMemberService circleMemberService;

    @Autowired
    private CircleService circleService;

    /**
     * 获取圈子成员列表
     * GET /api/circle/{circleId}/members
     */
    @Operation(summary = "获取圈子成员列表", description = "获取指定圈子的所有成员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取成员列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleMemberResponse.class))))
    })
    @GetMapping("/{circleId}/members")
    public ResponseEntity<?> getMembersByCircleId(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId) {
        try {
            requireApprovedPublicCircle(circleId);
            var members = circleMemberService.getMembersByCircleId(circleId);
            var responses = circleMemberService.convertToResponseList(members);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取圈子管理员列表
     * GET /api/circle/{circleId}/admins
     */
    @Operation(summary = "获取圈子管理员列表", description = "获取指定圈子的所有管理员列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取管理员列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleMemberResponse.class))))
    })
    @GetMapping("/{circleId}/admins")
    public ResponseEntity<?> getAdminsByCircleId(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId) {
        try {
            requireApprovedPublicCircle(circleId);
            var admins = circleMemberService.getAdminsByCircleId(circleId);
            var responses = circleMemberService.convertToResponseList(admins);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 加入圈子
     * POST /api/circle/{circleId}/join
     */
    @Operation(summary = "加入圈子", description = "当前用户加入指定圈子")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功加入圈子",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircleMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PostMapping("/{circleId}/join")
    public ResponseEntity<?> joinCircle(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId,
            @Parameter(description = "用户 ID", required = false)
            @RequestBody(required = false) Map<String, Long> requestBody) {
        try {
            requireApprovedPublicCircle(circleId);
            Long userId = requireCurrentUserId();
            var member = circleMemberService.joinCircle(circleId, userId);
            var response = circleMemberService.convertToResponse(member);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 退出圈子
     * POST /api/circle/{circleId}/leave
     */
    @Operation(summary = "退出圈子", description = "当前用户退出指定圈子")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功退出圈子",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PostMapping("/{circleId}/leave")
    public ResponseEntity<?> leaveCircle(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId,
            @Parameter(description = "用户 ID", required = false)
            @RequestBody(required = false) Map<String, Long> requestBody) {
        try {
            Long userId = requireCurrentUserId();
            circleMemberService.leaveCircle(circleId, userId);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 设置管理员
     * PUT /api/circle/{circleId}/members/{userId}/role
     */
    @Operation(summary = "设置成员角色", description = "设置圈子成员的角色（管理员、普通成员等）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功设置角色",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircleMemberResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PutMapping("/{circleId}/members/{userId}/role")
    public ResponseEntity<?> setAdminRole(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId,
            @Parameter(description = "用户 ID", required = true, example = "2")
            @PathVariable Long userId,
            @Parameter(description = "角色类型", required = true)
            @RequestBody Map<String, String> requestBody) {
        try {
            requireCircleManageActor(circleId);
            String role = requestBody.get("role");
            if (role == null) {
                throw new IllegalArgumentException("角色不能为空");
            }

            var member = circleMemberService.setAdminRole(circleId, userId, role);
            var response = circleMemberService.convertToResponse(member);
            return ResponseEntity.ok(response);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 移除成员
     * DELETE /api/circle/{circleId}/members/{userId}
     */
    @Operation(summary = "移除成员", description = "从圈子中移除指定用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功移除成员",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @DeleteMapping("/{circleId}/members/{userId}")
    public ResponseEntity<?> removeMember(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId,
            @Parameter(description = "用户 ID", required = true, example = "2")
            @PathVariable Long userId) {
        try {
            requireCircleManageActor(circleId);
            circleMemberService.removeMember(circleId, userId);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 检查用户是否是成员
     * GET /api/circle/{circleId}/members/{userId}/check
     */
    @Operation(summary = "检查用户是否是成员", description = "检查指定用户是否是圈子成员")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功检查",
                    content = @Content(mediaType = "application/json"))
    })
    @GetMapping("/{circleId}/members/{userId}/check")
    public ResponseEntity<?> isMember(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId,
            @Parameter(description = "用户 ID", required = true, example = "2")
            @PathVariable Long userId) {
        try {
            boolean isMember = circleMemberService.isMember(circleId, userId);
            Map<String, Boolean> result = new HashMap<>();
            result.put("isMember", isMember);
            return ResponseEntity.ok(result);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private Circle requireApprovedPublicCircle(Long circleId) {
        Circle circle = circleService.getCircleById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "圈子不存在或不可访问"));
        if (!"approved".equalsIgnoreCase(circle.getType()) || !"public".equalsIgnoreCase(circle.getVisibility())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "圈子不存在或不可访问");
        }
        return circle;
    }

    private void requireCircleManageActor(Long circleId) {
        Circle circle = circleService.getCircleById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "圈子不存在"));
        Long currentUserId = requireCurrentUserId();
        if (Objects.equals(circle.getCreatorId(), currentUserId)) {
            return;
        }

        boolean isAdmin = circleMemberService.getAdminsByCircleId(circleId).stream()
                .anyMatch(member -> member.getUser() != null && Objects.equals(member.getUser().getId(), currentUserId));
        if (!isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅圈主或圈子管理员可执行该操作");
        }
    }

    private Long requireCurrentUserId() {
        try {
            return UserUtil.getCurrentUserId();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
    }

    private ResponseEntity<Map<String, String>> buildErrorResponse(HttpStatusCode status, String message) {
        Map<String, String> error = new HashMap<>();
        error.put("message", message != null ? message : "请求处理失败");
        return ResponseEntity.status(status).body(error);
    }

    private String resolveReason(ResponseStatusException exception) {
        return exception.getReason() != null ? exception.getReason() : exception.getMessage();
    }
}
