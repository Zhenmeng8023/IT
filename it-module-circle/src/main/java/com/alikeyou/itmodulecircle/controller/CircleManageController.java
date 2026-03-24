package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecircle.service.CircleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.RequestParam;


import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/circle/manage")
@Tag(name = "圈子管理后台", description = "圈子管理、成员管理等后台操作接口")
public class CircleManageController {

    @Autowired
    private CircleService circleService;

    @Autowired
    private CircleMemberService circleMemberService;

    @Autowired
    private com.alikeyou.itmodulecircle.repository.CircleRepository circleRepository;

    @Autowired
    private com.alikeyou.itmodulecircle.repository.CircleMemberRepository circleMemberRepository;

    @Autowired
    private com.alikeyou.itmodulecircle.repository.CircleCommentRepository circleCommentRepository;


    // 圈子管理接口


    @GetMapping("/list")
    @Operation(summary = "获取圈子列表（管理端）", description = "支持分页和筛选条件")
    public ResponseEntity<Page<CircleResponse>> getCircleList(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Boolean recommended) {
        try {
            Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));

            Page<Circle> circles;
            if (type != null || visibility != null) {
                // 只支持 type 和 visibility 筛选
                List<Circle> allCircles = circleRepository.findAllByOrderByCreatedAtDesc();

                List<Circle> filteredCircles = allCircles.stream()
                        .filter(circle -> type == null || circle.getType().equals(type))
                        .filter(circle -> visibility == null || circle.getVisibility().equals(visibility))
                        .toList();

                // 应用分页
                int start = Math.min((int)pageable.getOffset(), filteredCircles.size());
                int end = Math.min((start + pageable.getPageSize()), filteredCircles.size());

                List<Circle> pagedContent = filteredCircles.subList(start, end);
                circles = new org.springframework.data.domain.PageImpl<>(pagedContent, pageable, filteredCircles.size());
            } else {
                // 没有筛选条件时，使用 repository 查询所有圈子
                List<Circle> allCircles = circleRepository.findAllByOrderByCreatedAtDesc();
                int start = Math.min((int)pageable.getOffset(), allCircles.size());
                int end = Math.min((start + pageable.getPageSize()), allCircles.size());

                List<Circle> pagedContent = allCircles.subList(start, end);
                circles = new org.springframework.data.domain.PageImpl<>(pagedContent, pageable, allCircles.size());
            }

            // 为每个圈子添加统计信息
            List<CircleResponse> responsesWithStats = circles.getContent().stream()
                    .map(circle -> {
                        CircleResponse response = circleService.convertToResponse(circle);
                        Map<String, Long> stats = circleService.getCircleStatisticsById(circle.getId());
                        if (stats != null) {
                            response.setMemberCount(stats.get("memberCount"));
                            response.setActiveMemberCount(stats.get("activeMemberCount"));
                            response.setPostCount(stats.get("postCount"));
                        } else {
                            response.setMemberCount(0L);
                            response.setActiveMemberCount(0L);
                            response.setPostCount(0L);
                        }
                        return response;
                    })
                    .collect(java.util.stream.Collectors.toList());
            Page<CircleResponse> responsePage = new org.springframework.data.domain.PageImpl<>(
                    responsesWithStats,
                    pageable,
                    circles.getTotalElements()
            );

            return ResponseEntity.ok(responsePage);
        } catch (Exception e) {
            e.printStackTrace();
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取圈子列表失败：" + e.getMessage());
            return ResponseEntity.badRequest().body(null);
        }
    }

    @GetMapping("/stats")
    @Operation(summary = "获取圈子统计信息")
    public ResponseEntity<Map<String, Object>> getStats() {
        try {
            // 使用 CircleService 的统计方法
            com.alikeyou.itmodulecircle.dto.CircleStatistics statistics = circleService.getCircleStatistics();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalCircles", statistics.getTotalCircles());
            stats.put("totalMembers", statistics.getTotalMembers());
            stats.put("activeMembers", statistics.getActiveMembers());
            stats.put("totalPosts", statistics.getTotalPosts());

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            Map<String, Object> error = new HashMap<>();
            error.put("message", "获取统计信息失败：" + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(error);
        }
    }

    @PutMapping("/approve/{id}")
    @Operation(summary = "审核通过圈子")
    public ResponseEntity<Map<String, String>> approveCircle(@PathVariable Long id) {
        try {
            // 由于数据库中没有 status 字段，此功能无法实现
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能不支持：数据库缺少必要字段");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "审核圈子失败");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping("/toggle-recommend/{id}")
    @Operation(summary = "切换推荐状态")
    public ResponseEntity<Map<String, String>> toggleRecommend(@PathVariable Long id) {
        try {
            // 由于数据库中没有 is_recommended 字段，此功能无法实现
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能不支持：数据库缺少必要字段");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "切换推荐状态失败");
            return ResponseEntity.internalServerError().body(error);
        }
    }

    @PutMapping("/close/{id}")
    @Operation(summary = "关闭圈子")
    public ResponseEntity<Map<String, String>> closeCircle(@PathVariable Long id) {
        try {
            // 由于数据库中没有 status 字段，此功能无法实现
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能不支持：数据库缺少必要字段");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(error);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "关闭圈子失败");
            return ResponseEntity.internalServerError().body(error);
        }
    }


    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除圈子")
    public ResponseEntity<Void> deleteCircle(@PathVariable Long id) {
        try {
            circleService.deleteCircle(id);
            return ResponseEntity.noContent().build();
        } catch (CircleException e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", e.getMessage());
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "删除圈子失败");
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审核通过")
    public ResponseEntity<Map<String, Object>> batchApprove(@RequestBody List<Long> ids) {
        try {
            // 由于数据库中没有 status 字段，此功能无法实现
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能不支持：数据库缺少必要字段");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "批量审核失败");
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/batch-close")
    @Operation(summary = "批量关闭")
    public ResponseEntity<Map<String, Object>> batchClose(@RequestBody List<Long> ids) {
        try {
            // 由于数据库中没有 status 字段，此功能无法实现
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能不支持：数据库缺少必要字段");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "批量关闭失败");
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除")
    public ResponseEntity<Map<String, Object>> batchDelete(@RequestBody List<Long> ids) {
        try {
            int successCount = 0;
            for (Long id : ids) {
                try {
                    circleService.deleteCircle(id);
                    successCount++;
                } catch (Exception e) {
                    // 继续处理其他ID
                }
            }
            Map<String, Object> result = new HashMap<>();
            result.put("successCount", successCount);
            result.put("totalCount", ids.size());
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "批量删除失败");
            return ResponseEntity.internalServerError().body(null);
        }
    }

    // 成员管理接口

    @GetMapping("/members/{circleId}")
    @Operation(summary = "获取圈子成员列表")
    public ResponseEntity<List<CircleMemberResponse>> getMembers(@PathVariable Long circleId) {
        try {
            List<CircleMember> members = circleMemberService.getMembersByCircleId(circleId);
            List<CircleMemberResponse> responses = circleMemberService.convertToResponseList(members);
            return ResponseEntity.ok(responses);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取成员列表失败");
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/set-admin/{memberId}")
    @Operation(summary = "设置管理员")
    public ResponseEntity<CircleMemberResponse> setAdmin(@PathVariable Long memberId, @RequestParam String role) {
        try {
            // 由于CircleMemberService没有提供通过ID查询的方法，我们需要使用其他方式
            // 这里我们假设memberId是CircleMember的ID，但需要先从请求中获取circleId和userId
            // 或者我们可以创建一个新的端点，接受circleId和userId作为参数
            Map<String, String> error = new HashMap<>();
            error.put("message", "功能暂未完全实现，请使用circleId和userId参数");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "设置管理员失败");
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/remove-member/{memberId}")
    @Operation(summary = "移除成员")
    public ResponseEntity<Void> removeMember(@PathVariable Long memberId) {
        try {
            // 由于CircleMemberService没有提供通过ID查询的方法，我们需要使用其他方式
            // 这里我们假设memberId是CircleMember的ID，但需要先从请求中获取circleId和userId
            // 或者我们可以创建一个新的端点，接受circleId和userId作为参数
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "移除成员失败");
            return ResponseEntity.badRequest().build();
        }
    }

    // 帖子管理接口（占位符，待实现）

    @GetMapping("/posts/{circleId}")
    @Operation(summary = "获取圈子帖子列表")
    public ResponseEntity<List<Object>> getPosts(@PathVariable Long circleId) {
        try {
            // TODO: 实现帖子管理功能
            // 目前缺少CirclePost相关实体和服务类
            Map<String, String> error = new HashMap<>();
            error.put("message", "帖子管理功能尚未实现");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "获取帖子列表失败");
            return ResponseEntity.badRequest().body(null);
        }
    }

    @PutMapping("/approve-post/{postId}")
    @Operation(summary = "审核通过帖子")
    public ResponseEntity<Object> approvePost(@PathVariable Long postId) {
        try {
            // TODO: 实现帖子管理功能
            // 目前缺少CirclePost相关实体和服务类
            Map<String, String> error = new HashMap<>();
            error.put("message", "帖子管理功能尚未实现");
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).body(null);
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "审核帖子失败");
            return ResponseEntity.badRequest().body(null);
        }
    }

    @DeleteMapping("/delete-post/{postId}")
    @Operation(summary = "删除帖子")
    public ResponseEntity<Void> deletePost(@PathVariable Long postId) {
        try {
            // TODO: 实现帖子管理功能
            // 目前缺少CirclePost相关实体和服务类
            return ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build();
        } catch (Exception e) {
            Map<String, String> error = new HashMap<>();
            error.put("message", "删除帖子失败");
            return ResponseEntity.badRequest().build();
        }
    }

}