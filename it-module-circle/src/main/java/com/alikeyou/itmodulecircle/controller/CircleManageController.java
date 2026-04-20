package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleCreatorInfo;
import com.alikeyou.itmodulecircle.dto.CircleManageApiResponse;
import com.alikeyou.itmodulecircle.dto.CircleManageBatchResult;
import com.alikeyou.itmodulecircle.dto.CircleManageCircleItemResponse;
import com.alikeyou.itmodulecircle.dto.CircleManagePageData;
import com.alikeyou.itmodulecircle.dto.CircleManagePostResponse;
import com.alikeyou.itmodulecircle.dto.CircleManageStatsResponse;
import com.alikeyou.itmodulecircle.dto.CircleMemberResponse;
import com.alikeyou.itmodulecircle.dto.CircleResponse;
import com.alikeyou.itmodulecircle.dto.CircleCloseRequest;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.entity.CircleMember;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.service.CircleCommentService;
import com.alikeyou.itmodulecircle.service.CircleMemberService;
import com.alikeyou.itmodulecircle.service.CircleService;
import com.alikeyou.itmodulecircle.support.CircleCommentVisibilitySupport;
import com.alikeyou.itmodulecircle.support.CircleLifecycleCompat;
import com.alikeyou.itmodulecircle.support.CircleMessageNormalizer;
import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmodulelogin.repository.UserRepository;
import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@RestController
@RequestMapping("/api/circle/manage")
@Tag(name = "圈子管理后台", description = "圈子管理、成员管理、帖子审核等后台操作接口")
public class CircleManageController {

    private static final Set<Integer> MANAGE_ROLE_IDS = Set.of(1, 2, 3);
    private static final Set<String> MANAGE_MEMBER_ROLES = Set.of("admin", "moderator", "member");
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final int DEFAULT_VIEW_COUNT = 0;

    @Autowired
    private CircleService circleService;

    @Autowired
    private CircleMemberService circleMemberService;

    @Autowired
    private CircleCommentService circleCommentService;

    @Autowired
    private UserRepository userRepository;

    @GetMapping("/list")
    @Operation(summary = "获取圈子列表（管理端）", description = "统一返回 code/message/data 包装，支持分页和筛选")
    public ResponseEntity<CircleManageApiResponse<CircleManagePageData<CircleManageCircleItemResponse>>> getCircleList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(name = "pageSize", required = false) Integer pageSize,
            @RequestParam(required = false) String lifecycle,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String privacy,
            @RequestParam(required = false) String visibility,
            @RequestParam(required = false) Boolean recommended,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        return execute("获取圈子列表成功", () -> {
            requireManagePermission();

            if (recommended != null) {
                throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                        "推荐筛选已下线：当前数据结构不支持推荐字段");
            }

            int safePage = normalizePage(page);
            int requestedPageSize = pageSize != null ? pageSize : size;
            int safeSize = normalizePageSize(requestedPageSize);
            String lifecycleFilter = resolveLifecycleFilter(status, lifecycle, type);
            String visibilityFilter = resolveVisibilityFilter(privacy, visibility);
            Instant startTime = parseDateStart(startDate);
            Instant endTime = parseDateEnd(endDate);

            List<CircleManageCircleItemResponse> filtered = circleService.getAllCircles().stream()
                    .map(this::toManageCircleItem)
                    .filter(item -> lifecycleFilter == null || lifecycleFilter.equals(item.getType()))
                    .filter(item -> visibilityFilter == null || visibilityFilter.equalsIgnoreCase(item.getVisibility()))
                    .filter(item -> matchKeyword(item, keyword))
                    .filter(item -> matchTimeRange(item.getCreatedAt(), startTime, endTime))
                    .toList();

            int from = Math.min((safePage - 1) * safeSize, filtered.size());
            int to = Math.min(from + safeSize, filtered.size());
            List<CircleManageCircleItemResponse> pageList = filtered.subList(from, to);

            CircleManagePageData<CircleManageCircleItemResponse> pageData = new CircleManagePageData<>();
            pageData.setList(pageList);
            pageData.setTotal((long) filtered.size());
            pageData.setCurrentPage(safePage);
            pageData.setPageSize(safeSize);
            return pageData;
        });
    }

    @GetMapping("/stats")
    @Operation(summary = "获取圈子统计信息（管理端）")
    public ResponseEntity<CircleManageApiResponse<CircleManageStatsResponse>> getStats() {
        return execute("获取统计信息成功", () -> {
            requireManagePermission();

            com.alikeyou.itmodulecircle.dto.CircleStatistics statistics = circleService.getCircleStatistics();
            CircleManageStatsResponse response = new CircleManageStatsResponse();
            response.setTotalCircles(defaultLong(statistics.getTotalCircles()));
            response.setTotalMembers(defaultLong(statistics.getTotalMembers()));
            response.setActiveMembers(defaultLong(statistics.getActiveMembers()));
            response.setTotalPosts(defaultLong(statistics.getTotalPosts()));
            response.setTodayActive(defaultLong(statistics.getActiveMembers()));
            return response;
        });
    }

    @RequestMapping(value = "/approve/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "审核通过圈子")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> approveCircle(@PathVariable Long id) {
        return execute("审核通过成功", () -> {
            requireManagePermission();
            validateId(id, "圈子 ID");

            circleService.approveCircle(id);
            Circle circle = circleService.getCircleById(id)
                    .orElseThrow(() -> new CircleException("圈子不存在"));
            return buildCircleActionData(circle, "approve");
        });
    }

    @RequestMapping(value = "/reject/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "拒绝圈子审核")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> rejectCircle(@PathVariable Long id) {
        return execute("拒绝审核成功", () -> {
            requireManagePermission();
            validateId(id, "圈子 ID");

            circleService.rejectCircle(id);
            Circle circle = circleService.getCircleById(id)
                    .orElseThrow(() -> new CircleException("圈子不存在"));
            return buildCircleActionData(circle, "reject");
        });
    }

    @RequestMapping(value = "/toggle-recommend/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "推荐功能（已下线）")
    public ResponseEntity<CircleManageApiResponse<Object>> toggleRecommend(@PathVariable Long id) {
        return execute("推荐功能已下线", () -> {
            requireManagePermission();
            validateId(id, "圈子 ID");
            throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED,
                    "推荐功能已下线：circle 数据模型缺少推荐字段，当前版本不支持推荐/取消推荐");
        });
    }

    @RequestMapping(value = "/close/{id}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "关闭圈子")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> closeCircle(@PathVariable Long id) {
        return execute("关闭圈子成功", () -> {
            requireManagePermission();
            validateId(id, "圈子 ID");
            Long operatorId = requireCurrentUserId();

            closeCircleByManage(id, operatorId, "后台管理关闭圈子");
            Circle circle = circleService.getCircleById(id)
                    .orElseThrow(() -> new CircleException("圈子不存在"));
            return buildCircleActionData(circle, "close");
        });
    }

    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除圈子")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> deleteCircle(@PathVariable Long id) {
        return execute("删除圈子成功", () -> {
            requireManagePermission();
            validateId(id, "圈子 ID");
            Long operatorId = requireCurrentUserId();

            circleService.deleteCircle(id, operatorId);
            return Map.of(
                    "id", id,
                    "deleted", true
            );
        });
    }

    @PostMapping("/batch-approve")
    @Operation(summary = "批量审核通过圈子")
    public ResponseEntity<CircleManageApiResponse<CircleManageBatchResult>> batchApprove(@RequestBody(required = false) JsonNode payload) {
        return execute("批量审核通过执行完成", () -> {
            requireManagePermission();
            List<Long> ids = parseIds(payload, "圈子");

            return executeBatch(ids, circleService::approveCircle);
        });
    }

    @PostMapping("/batch-reject")
    @Operation(summary = "批量拒绝圈子审核")
    public ResponseEntity<CircleManageApiResponse<CircleManageBatchResult>> batchReject(@RequestBody(required = false) JsonNode payload) {
        return execute("批量拒绝执行完成", () -> {
            requireManagePermission();
            List<Long> ids = parseIds(payload, "圈子");

            return executeBatch(ids, circleService::rejectCircle);
        });
    }

    @PostMapping("/batch-close")
    @Operation(summary = "批量关闭圈子")
    public ResponseEntity<CircleManageApiResponse<CircleManageBatchResult>> batchClose(@RequestBody(required = false) JsonNode payload) {
        return execute("批量关闭执行完成", () -> {
            requireManagePermission();
            Long operatorId = requireCurrentUserId();
            List<Long> ids = parseIds(payload, "圈子");

            return executeBatch(ids, id -> closeCircleByManage(id, operatorId, "后台批量关闭圈子"));
        });
    }

    @PostMapping("/batch-delete")
    @Operation(summary = "批量删除圈子")
    public ResponseEntity<CircleManageApiResponse<CircleManageBatchResult>> batchDelete(@RequestBody(required = false) JsonNode payload) {
        return execute("批量删除执行完成", () -> {
            requireManagePermission();
            Long operatorId = requireCurrentUserId();
            List<Long> ids = parseIds(payload, "圈子");

            return executeBatch(ids, id -> circleService.deleteCircle(id, operatorId));
        });
    }

    @GetMapping("/members/{circleId}")
    @Operation(summary = "获取圈子成员列表（管理端）")
    public ResponseEntity<CircleManageApiResponse<List<CircleMemberResponse>>> getMembers(@PathVariable Long circleId) {
        return execute("获取成员列表成功", () -> {
            requireManagePermission();
            validateId(circleId, "圈子 ID");

            List<CircleMember> members = circleMemberService.getMembersByCircleId(circleId);
            return members.stream()
                    .map(this::toManageMemberResponse)
                    .toList();
        });
    }

    @RequestMapping(value = "/set-admin/{memberId}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "设置成员角色（管理端）")
    public ResponseEntity<CircleManageApiResponse<CircleMemberResponse>> setAdmin(
            @PathVariable Long memberId,
            @RequestParam(required = false) String role) {
        return execute("设置成员角色成功", () -> {
            requireManagePermission();
            validateId(memberId, "成员关系 ID");
            Long operatorId = requireCurrentUserId();

            String targetRole = normalizeManageRole(role);
            CircleMember updatedMember = circleMemberService.setMemberRoleByMemberId(memberId, targetRole, operatorId);
            return toManageMemberResponse(updatedMember);
        });
    }

    @RequestMapping(value = "/remove-member/{memberId}", method = {RequestMethod.DELETE, RequestMethod.POST})
    @Operation(summary = "移除圈子成员（管理端）")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> removeMember(@PathVariable Long memberId) {
        return execute("移除成员成功", () -> {
            requireManagePermission();
            validateId(memberId, "成员关系 ID");
            Long operatorId = requireCurrentUserId();

            CircleMember member = circleMemberService.getMemberById(memberId)
                    .orElseThrow(() -> new CircleException("成员关系不存在"));
            circleMemberService.removeMemberByMemberId(memberId, operatorId);
            Map<String, Object> result = new java.util.LinkedHashMap<>();
            result.put("id", memberId);
            result.put("circleId", member.getCircle() != null ? member.getCircle().getId() : null);
            result.put("userId", member.getUser() != null ? member.getUser().getId() : null);
            result.put("removed", true);
            return result;
        });
    }

    @GetMapping("/posts/{circleId}")
    @Operation(summary = "获取圈子帖子列表（管理端）")
    public ResponseEntity<CircleManageApiResponse<List<CircleManagePostResponse>>> getPosts(@PathVariable Long circleId) {
        return execute("获取帖子列表成功", () -> {
            requireManagePermission();
            validateId(circleId, "圈子 ID");

            List<CircleComment> posts = circleCommentService.getManagePostsByCircleId(circleId);
            return posts.stream()
                    .map(this::toManagePostResponse)
                    .toList();
        });
    }

    @RequestMapping(value = "/approve-post/{postId}", method = {RequestMethod.PUT, RequestMethod.POST})
    @Operation(summary = "审核通过帖子（管理端）")
    public ResponseEntity<CircleManageApiResponse<CircleManagePostResponse>> approvePost(@PathVariable Long postId) {
        return execute("帖子审核通过成功", () -> {
            requireManagePermission();
            validateId(postId, "帖子 ID");

            CircleComment approvedPost = circleCommentService.approvePost(postId);
            return toManagePostResponse(approvedPost);
        });
    }

    @DeleteMapping("/delete-post/{postId}")
    @Operation(summary = "删除帖子（管理端）")
    public ResponseEntity<CircleManageApiResponse<Map<String, Object>>> deletePost(@PathVariable Long postId) {
        return execute("帖子删除成功", () -> {
            requireManagePermission();
            validateId(postId, "帖子 ID");

            CircleComment existingPost = circleCommentService.getCommentById(postId)
                    .orElseThrow(() -> new CircleException("帖子不存在"));
            if (existingPost.getParentCommentId() != null) {
                throw new CircleException("仅支持删除主题帖，当前 ID 不是主题帖");
            }

            Long rootPostId = existingPost.getPostId() != null ? existingPost.getPostId() : existingPost.getId();
            circleCommentService.deletePostWithReplies(postId);

            return Map.of(
                    "id", postId,
                    "postId", rootPostId,
                    "deleted", true
            );
        });
    }

    private CircleManageCircleItemResponse toManageCircleItem(Circle circle) {
        CircleResponse response = circleService.convertToResponse(circle);

        String lifecycle = circleService.getLifecycleStatus(circle);
        String compatibilityStatus = mapLifecycleToCompatibilityStatus(lifecycle);

        CircleManageCircleItemResponse item = new CircleManageCircleItemResponse();
        item.setId(response.getId());
        item.setName(response.getName());
        item.setDescription(response.getDescription());
        item.setType(lifecycle);
        item.setStatus(compatibilityStatus);
        item.setVisibility(response.getVisibility());
        item.setPrivacy(response.getVisibility());
        item.setMaxMembers(response.getMaxMembers());
        item.setMemberCount(defaultLong(response.getMemberCount()));
        item.setActiveMemberCount(defaultLong(response.getActiveMemberCount()));
        item.setPostCount(defaultLong(response.getPostCount()));
        item.setTodayActive(defaultLong(response.getActiveMemberCount()));
        item.setCreatedAt(response.getCreatedAt());
        item.setCreateTime(response.getCreatedAt());
        item.setUpdatedAt(response.getUpdatedAt());
        item.setIsRecommended(Boolean.FALSE);

        CircleCreatorInfo creatorInfo = response.getCreator();
        if (creatorInfo != null) {
            item.setCreatorId(creatorInfo.getId());
            item.setCreator(creatorInfo.getUsername());
            item.setCreatorName(creatorInfo.getUsername());
            item.setCreatorAvatar(creatorInfo.getAvatar());
            item.setCreatorAvatarUrl(creatorInfo.getAvatar());
            item.setCreatorInfo(creatorInfo);
        } else {
            item.setCreatorId(circle.getCreatorId());
            item.setCreator("未知用户");
            item.setCreatorName("未知用户");
        }
        return item;
    }

    private CircleMemberResponse toManageMemberResponse(CircleMember member) {
        CircleMemberResponse response = circleMemberService.convertToResponse(member);
        String role = response.getRole();
        if ("owner".equalsIgnoreCase(role)) {
            response.setRole("creator");
        } else if (role != null) {
            response.setRole(role.toLowerCase());
        }

        if (response.getLastActive() == null) {
            response.setLastActive(response.getJoinTime());
        }
        if (response.getAvatar() == null || response.getAvatar().isBlank()) {
            response.setAvatar(response.getAvatarUrl());
        }
        return response;
    }

    private CircleManagePostResponse toManagePostResponse(CircleComment post) {
        Long rootPostId = post.getPostId() != null ? post.getPostId() : post.getId();
        String authorName = resolveAuthorName(post.getAuthor());

        CircleManagePostResponse response = new CircleManagePostResponse();
        response.setId(post.getId());
        response.setCircleId(post.getCircleId());
        response.setPostId(rootPostId);
        response.setAuthorId(post.getAuthor() != null ? post.getAuthor().getId() : null);
        response.setAuthor(authorName);
        response.setAuthorName(authorName);
        response.setAuthorAvatar(post.getAuthor() != null ? post.getAuthor().getAvatarUrl() : null);
        response.setAuthorAvatarUrl(post.getAuthor() != null ? post.getAuthor().getAvatarUrl() : null);
        response.setContent(post.getContent());
        response.setTitle(buildPostTitle(post.getContent()));
        response.setStatus(normalizePostStatus(post.getStatus()));
        response.setViewCount(DEFAULT_VIEW_COUNT);
        response.setCommentCount(circleCommentService.countRepliesByPostId(rootPostId));
        response.setLikes(post.getLikes() == null ? 0 : post.getLikes());
        response.setCreatedAt(post.getCreatedAt());
        response.setCreateTime(post.getCreatedAt());
        return response;
    }

    private String buildPostTitle(String content) {
        if (content == null || content.isBlank()) {
            return "无标题";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() <= 30 ? normalized : normalized.substring(0, 30) + "...";
    }

    private String resolveAuthorName(UserInfo author) {
        if (author == null) {
            return "未知用户";
        }
        if (author.getNickname() != null && !author.getNickname().isBlank()) {
            return author.getNickname();
        }
        if (author.getUsername() != null && !author.getUsername().isBlank()) {
            return author.getUsername();
        }
        return "未知用户";
    }

    private String normalizePostStatus(String status) {
        return CircleCommentVisibilitySupport.normalizeStatus(status);
    }

    private boolean matchKeyword(CircleManageCircleItemResponse item, String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return true;
        }
        String normalized = keyword.trim().toLowerCase();
        String name = item.getName() == null ? "" : item.getName().toLowerCase();
        String creator = item.getCreator() == null ? "" : item.getCreator().toLowerCase();
        String description = item.getDescription() == null ? "" : item.getDescription().toLowerCase();
        return name.contains(normalized) || creator.contains(normalized) || description.contains(normalized);
    }

    private boolean matchTimeRange(Instant createdAt, Instant start, Instant end) {
        if (createdAt == null) {
            return start == null && end == null;
        }
        if (start != null && createdAt.isBefore(start)) {
            return false;
        }
        if (end != null && createdAt.isAfter(end)) {
            return false;
        }
        return true;
    }

    private Instant parseDateStart(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).atStartOfDay().toInstant(ZoneOffset.UTC);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日期格式错误，startDate 应为 yyyy-MM-dd");
        }
    }

    private Instant parseDateEnd(String date) {
        if (date == null || date.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(date).plusDays(1).atStartOfDay().toInstant(ZoneOffset.UTC).minusMillis(1);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "日期格式错误，endDate 应为 yyyy-MM-dd");
        }
    }

    private String resolveLifecycleFilter(String status, String lifecycle, String type) {
        if (status != null && !status.isBlank()) {
            return normalizeLifecycleCandidate(status, true);
        }
        if (lifecycle != null && !lifecycle.isBlank()) {
            return normalizeLifecycleCandidate(lifecycle, true);
        }
        if (type != null && !type.isBlank()) {
            // 兼容老前端仍把状态筛选塞到 type；若传的是圈子分类词（如 tech/study）则忽略，避免误报 400
            return normalizeLifecycleCandidate(type, false);
        }
        return null;
    }

    private String normalizeLifecycleCandidate(String candidate, boolean strict) {
        try {
            return CircleLifecycleCompat.normalizeLifecycleFilter(candidate, strict);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private String resolveVisibilityFilter(String privacy, String visibility) {
        String candidate = null;
        if (privacy != null && !privacy.isBlank()) {
            candidate = privacy;
        } else if (visibility != null && !visibility.isBlank()) {
            candidate = visibility;
        }

        if (candidate == null) {
            return null;
        }

        String normalized = candidate.trim().toLowerCase();
        if ("public".equals(normalized) || "private".equals(normalized) || "approval".equals(normalized)) {
            return normalized;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "无效隐私筛选值，仅支持 public/private/approval");
    }

    private String normalizeLifecycleType(String type) {
        return CircleLifecycleCompat.getLifecycleStatus(type, null);
    }

    private String mapLifecycleToCompatibilityStatus(String lifecycle) {
        return CircleLifecycleCompat.toCompatibilityStatus(lifecycle);
    }

    private String normalizeManageRole(String role) {
        if (role == null || role.isBlank()) {
            return "admin";
        }
        String normalized = role.trim().toLowerCase();
        if (!MANAGE_MEMBER_ROLES.contains(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "角色参数无效，仅支持：admin、moderator、member");
        }
        return normalized;
    }

    private void closeCircleByManage(Long id, Long operatorId, String reason) {
        CircleCloseRequest request = new CircleCloseRequest();
        request.setOperatorId(operatorId);
        request.setReason(reason);
        circleService.closeCircleWithDetail(id, request);
    }

    private Map<String, Object> buildCircleActionData(Circle circle, String action) {
        String lifecycle = circleService.getLifecycleStatus(circle);
        return Map.of(
                "id", circle.getId(),
                "action", action,
                "type", lifecycle,
                "status", mapLifecycleToCompatibilityStatus(lifecycle),
                "updatedAt", circle.getUpdatedAt()
        );
    }

    private CircleManageBatchResult executeBatch(List<Long> ids, BatchAction action) {
        CircleManageBatchResult result = new CircleManageBatchResult();
        result.setTotalCount(ids.size());

        int successCount = 0;
        List<Long> failedIds = new ArrayList<>();
        Map<Long, String> failedReason = new java.util.LinkedHashMap<>();

        for (Long id : ids) {
            try {
                action.apply(id);
                successCount++;
            } catch (Exception e) {
                failedIds.add(id);
                failedReason.put(id, buildErrorMessage(e));
            }
        }

        result.setSuccessCount(successCount);
        result.setFailedCount(failedIds.size());
        result.setFailedIds(failedIds);
        result.setFailedReason(failedReason);
        return result;
    }

    private List<Long> parseIds(JsonNode payload, String label) {
        JsonNode idsNode;
        if (payload == null || payload.isNull()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ID 列表不能为空");
        }

        if (payload.isArray()) {
            idsNode = payload;
        } else if (payload.isObject()) {
            idsNode = payload.get("ids");
        } else {
            idsNode = null;
        }

        if (idsNode == null || !idsNode.isArray()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "请求体格式错误，应传入 [1,2,3] 或 {\"ids\":[1,2,3]}");
        }

        LinkedHashSet<Long> uniqueIds = new LinkedHashSet<>();
        for (JsonNode idNode : idsNode) {
            if (!idNode.canConvertToLong()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ID 必须是数字");
            }
            long id = idNode.asLong();
            if (id <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ID 必须是正整数");
            }
            uniqueIds.add(id);
        }

        if (uniqueIds.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " ID 列表不能为空");
        }
        return new ArrayList<>(uniqueIds);
    }

    private int normalizePage(int page) {
        return page <= 0 ? 1 : page;
    }

    private int normalizePageSize(int size) {
        if (size <= 0) {
            return DEFAULT_PAGE_SIZE;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private void validateId(Long id, String label) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, label + " 必须是正整数");
        }
    }

    private Long requireCurrentUserId() {
        try {
            Long userId = UserUtil.getCurrentUserId();
            if (userId == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CircleMessageNormalizer.LOGIN_REQUIRED);
            }
            return userId;
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, CircleMessageNormalizer.LOGIN_REQUIRED);
        }
    }

    private void requireManagePermission() {
        Long userId = requireCurrentUserId();

        Integer roleId = LoginConstant.getRoleId();
        if (roleId == null) {
            roleId = userRepository.findById(userId)
                    .map(UserInfo::getRoleId)
                    .orElse(null);
        }

        if (roleId == null || !MANAGE_ROLE_IDS.contains(roleId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, CircleMessageNormalizer.PERMISSION_DENIED);
        }
    }

    private <T> ResponseEntity<CircleManageApiResponse<T>> execute(String successMessage, Supplier<T> supplier) {
        try {
            T data = supplier.get();
            return ResponseEntity.ok(CircleManageApiResponse.success(CircleMessageNormalizer.normalizeSuccess(successMessage), data));
        } catch (ResponseStatusException e) {
            return buildError(e.getStatusCode(), e.getReason());
        } catch (CircleException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (IllegalArgumentException e) {
            return buildError(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            return buildError(HttpStatus.INTERNAL_SERVER_ERROR, "服务器内部错误");
        }
    }

    private <T> ResponseEntity<CircleManageApiResponse<T>> buildError(HttpStatusCode status, String message) {
        HttpStatus resolvedStatus = CircleMessageNormalizer.resolveStatus(status, message);
        String normalizedMessage = CircleMessageNormalizer.normalize(resolvedStatus, message);
        return ResponseEntity.status(resolvedStatus)
                .body(CircleManageApiResponse.failure(resolvedStatus.value(), normalizedMessage));
    }

    private String buildErrorMessage(Exception e) {
        if (e instanceof ResponseStatusException responseStatusException) {
            HttpStatus resolvedStatus = CircleMessageNormalizer.resolveStatus(
                    responseStatusException.getStatusCode(),
                    responseStatusException.getReason()
            );
            return CircleMessageNormalizer.normalize(resolvedStatus, responseStatusException.getReason());
        }
        if (e instanceof CircleException circleException) {
            HttpStatus resolvedStatus = CircleMessageNormalizer.resolveStatus(HttpStatus.BAD_REQUEST, circleException.getMessage());
            return CircleMessageNormalizer.normalize(resolvedStatus, circleException.getMessage());
        }
        if (e.getMessage() != null && !e.getMessage().isBlank()) {
            HttpStatus resolvedStatus = CircleMessageNormalizer.resolveStatus(HttpStatus.BAD_REQUEST, e.getMessage());
            return CircleMessageNormalizer.normalize(resolvedStatus, e.getMessage());
        }
        return CircleMessageNormalizer.REQUEST_FAILED;
    }

    private Long defaultLong(Long value) {
        return value == null ? 0L : value;
    }

    @FunctionalInterface
    private interface BatchAction {
        void apply(Long id);
    }
}
