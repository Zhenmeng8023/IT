package com.alikeyou.itmodulecircle.controller;

import com.alikeyou.itmodulecircle.dto.CircleCommentRequest;
import com.alikeyou.itmodulecircle.dto.CircleCommentResponse;
import com.alikeyou.itmodulecircle.entity.Circle;
import com.alikeyou.itmodulecircle.entity.CircleComment;
import com.alikeyou.itmodulecircle.service.CircleCommentService;
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
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@RestController
@RequestMapping("/api/circle")
@Tag(name = "圈子评论管理", description = "圈子内帖子和评论的相关接口")
public class CircleCommentController {

    private static final String POST_STATUS_PENDING = "pending";
    private static final String POST_STATUS_PUBLISHED = "published";
    private static final String POST_STATUS_DELETED = "deleted";

    @Autowired
    private CircleCommentService circleCommentService;

    @Autowired
    private CircleService circleService;

    /**
     * 创建帖子或评论
     * POST /api/circle/comments
     */
    @Operation(summary = "创建帖子或评论", description = "在圈子内创建主题帖或回复评论")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功创建",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircleCommentResponse.class))),
            @ApiResponse(responseCode = "400", description = "请求参数无效",
                    content = @Content)
    })
    @PostMapping("/comments")
    public ResponseEntity<?> createComment(
            @Parameter(description = "评论请求对象", required = true)
            @RequestBody CircleCommentRequest request) {
        try {
            request.setAuthorId(requireCurrentUserId());
            validateCreateRequest(request);
            var comment = circleCommentService.createComment(request);
            var response = circleCommentService.convertToResponse(comment);
            return new ResponseEntity<>(response, HttpStatus.CREATED);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取帖子的一级回复列表
     * GET /api/circle/posts/{postId}/replies
     */
    @Operation(summary = "获取帖子的一级回复列表", description = "获取指定主题帖的直接回复（不包括楼中楼）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取回复列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/posts/{postId}/replies")
    public ResponseEntity<?> getDirectRepliesByPostId(
            @Parameter(description = "帖子 ID", required = true, example = "1")
            @PathVariable Long postId) {
        try {
            requireVisibleRootPost(postId);
            var replies = circleCommentService.getDirectRepliesByPostId(postId).stream()
                    .filter(this::isVisibleComment)
                    .toList();
            var responses = circleCommentService.convertToResponseList(replies);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取主题帖的所有评论
     * GET /api/circle/posts/{postId}/comments/all
     */
    @Operation(summary = "获取主题帖的所有评论", description = "获取指定主题帖的所有评论（包括一级回复和楼中楼），按创建时间升序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取评论列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/posts/{postId}/comments/all")
    public ResponseEntity<?> getAllCommentsByPostId(
            @Parameter(description = "帖子 ID", required = true, example = "1")
            @PathVariable Long postId) {
        try {
            requireVisibleRootPost(postId);
            var comments = circleCommentService.getAllCommentsByPostId(postId).stream()
                    .filter(this::isVisibleComment)
                    .toList();
            var responses = circleCommentService.convertToResponseList(comments);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取某条评论的子回复（楼中楼）
     * GET /api/circle/comments/{commentId}/replies
     */
    @Operation(summary = "获取评论的子回复", description = "获取指定评论的所有子回复（楼中楼）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取子回复列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/comments/{commentId}/replies")
    public ResponseEntity<?> getRepliesByCommentId(
            @Parameter(description = "评论 ID", required = true, example = "1")
            @PathVariable Long commentId) {
        try {
            var comment = circleCommentService.getCommentById(commentId)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在或不可访问"));
            requireVisibleRootPost(resolveRootPostId(comment));

            var replies = circleCommentService.getRepliesByCommentId(commentId).stream()
                    .filter(this::isVisibleComment)
                    .toList();
            var responses = circleCommentService.convertToResponseList(replies);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取圈子的主题帖列表
     * GET /api/circle/{circleId}/posts
     */
    @Operation(summary = "获取圈子的主题帖列表", description = "获取指定圈子的所有主题帖（不包括回复）")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取帖子列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/{circleId}/posts")
    public ResponseEntity<?> getPostsByCircleId(
            @Parameter(description = "圈子 ID", required = true, example = "1")
            @PathVariable Long circleId) {
        try {
            requireApprovedPublicCircle(circleId);
            var posts = circleCommentService.getPostsByCircleId(circleId).stream()
                    .filter(this::isVisiblePost)
                    .toList();
            var responses = circleCommentService.convertToResponseList(posts);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取评论详情
     * GET /api/circle/comments/{id}
     */
    @Operation(summary = "获取评论详情", description = "获取单条评论/帖子的详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取评论详情",
                    content = @Content(mediaType = "application/json",
                            schema = @Schema(implementation = CircleCommentResponse.class))),
            @ApiResponse(responseCode = "404", description = "评论不存在",
                    content = @Content)
    })
    @GetMapping("/comments/{id}")
    public ResponseEntity<?> getCommentById(
            @Parameter(description = "评论 ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            return circleCommentService.getCommentById(id)
                    .filter(this::isVisibleCommentWithPublicRoot)
                    .map(comment -> ResponseEntity.ok(circleCommentService.convertToResponse(comment)))
                    .orElse(ResponseEntity.notFound().build());
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 删除评论/帖子
     * DELETE /api/circle/comments/{id}
     */
    @Operation(summary = "删除评论/帖子", description = "删除指定的评论或帖子")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "评论不存在",
                    content = @Content)
    })
    @DeleteMapping("/comments/{id}")
    public ResponseEntity<?> deleteComment(
            @Parameter(description = "评论 ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            Long currentUserId = requireCurrentUserId();
            CircleComment comment = circleCommentService.getCommentById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在"));
            Long authorId = comment.getAuthor() != null ? comment.getAuthor().getId() : null;
            if (!Objects.equals(authorId, currentUserId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅作者本人可删除评论或帖子");
            }
            circleCommentService.deleteComment(id);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 点赞评论/帖子
     * POST /api/circle/comments/{id}/like
     */
    @Operation(summary = "点赞评论/帖子", description = "为指定的评论或帖子增加点赞计数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "点赞成功",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "评论不存在",
                    content = @Content)
    })
    @PostMapping("/comments/{id}/like")
    public ResponseEntity<?> incrementCommentLikes(
            @Parameter(description = "评论 ID", required = true, example = "1")
            @PathVariable Long id) {
        try {
            CircleComment comment = circleCommentService.getCommentById(id)
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在或不可访问"));
            if (!isVisibleCommentWithPublicRoot(comment)) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "评论不存在或不可访问");
            }
            circleCommentService.incrementLikes(id);
            return ResponseEntity.ok().build();
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
            * 根据用户 ID 获取主题帖列表
     * GET /api/circle/user/{userId}/posts
     */
    @Operation(summary = "根据用户 ID 获取主题帖列表", description = "获取指定用户在所有圈子中发布的主题帖")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取帖子列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/user/{userId}/posts")
    public ResponseEntity<?> getPostsByAuthorId(
            @Parameter(description = "用户 ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            Long currentUserId = tryGetCurrentUserId();
            var posts = circleCommentService.getPostsByAuthorId(userId).stream()
                    .filter(post -> Objects.equals(currentUserId, userId) || isVisiblePost(post))
                    .toList();
            var responses = circleCommentService.convertToResponseList(posts);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 根据用户 ID 删除主题帖
     * DELETE /api/circle/user/{userId}/posts
     */
    @Operation(summary = "根据用户 ID 删除主题帖", description = "删除指定用户在所有圈子中发布的所有主题帖")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除",
                    content = @Content),
            @ApiResponse(responseCode = "400", description = "删除失败",
                    content = @Content)
    })
    @DeleteMapping("/user/{userId}/posts")
    public ResponseEntity<?> deletePostsByAuthorId(
            @Parameter(description = "用户 ID", required = true, example = "1")
            @PathVariable Long userId) {
        try {
            Long currentUserId = requireCurrentUserId();
            if (!Objects.equals(currentUserId, userId)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "仅允许删除当前登录用户自己的帖子");
            }
            circleCommentService.deletePostsByAuthorId(userId);
            return ResponseEntity.noContent().build();
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 获取所有圈子的所有主题帖列表
     * GET /api/circle/posts/all
     */
    @Operation(summary = "获取所有圈子的所有主题帖列表", description = "获取系统中所有圈子的所有主题帖（不包括回复），按创建时间倒序排列")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取帖子列表",
                    content = @Content(mediaType = "application/json",
                            array = @io.swagger.v3.oas.annotations.media.ArraySchema(
                                    schema = @Schema(implementation = CircleCommentResponse.class))))
    })
    @GetMapping("/posts/all")
    public ResponseEntity<?> getAllPosts() {
        try {
            var posts = circleCommentService.getAllPosts().stream()
                    .filter(this::isVisiblePost)
                    .toList();
            var responses = circleCommentService.convertToResponseList(posts);
            return ResponseEntity.ok(responses);
        } catch (ResponseStatusException e) {
            return buildErrorResponse(e.getStatusCode(), resolveReason(e));
        } catch (Exception e) {
            return buildErrorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    private void validateCreateRequest(CircleCommentRequest request) {
        Circle circle = requireApprovedPublicCircle(request.getCircleId());
        if (request.getParentCommentId() == null) {
            return;
        }

        CircleComment parentComment = circleCommentService.getCommentById(request.getParentCommentId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "父评论不存在或不可访问"));

        Long rootPostId = resolveRootPostId(parentComment);
        CircleComment rootPost = requireVisibleRootPost(rootPostId);
        if (!Objects.equals(rootPost.getCircleId(), circle.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "评论所属圈子与主题帖不一致");
        }
        if (!isVisibleComment(parentComment)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "父评论不存在或不可访问");
        }
    }

    private CircleComment requireVisibleRootPost(Long postId) {
        CircleComment rootPost = circleCommentService.getCommentById(postId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在或不可访问"));

        if (rootPost.getParentCommentId() != null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在或不可访问");
        }

        if (!isVisiblePost(rootPost)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "帖子不存在或不可访问");
        }

        return rootPost;
    }

    private Circle requireApprovedPublicCircle(Long circleId) {
        Circle circle = circleService.getCircleById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "圈子不存在或不可访问"));
        if (!isApprovedPublicCircle(circle)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "圈子不存在或不可访问");
        }
        return circle;
    }

    private boolean isVisibleCommentWithPublicRoot(CircleComment comment) {
        if (!isVisibleComment(comment)) {
            return false;
        }

        return resolveRootPost(comment)
                .map(this::isVisiblePost)
                .orElse(false);
    }

    private boolean isVisibleComment(CircleComment comment) {
        if (comment == null) {
            return false;
        }

        String normalizedStatus = normalizePostStatus(comment.getStatus());
        if (POST_STATUS_DELETED.equals(normalizedStatus)) {
            return false;
        }

        if (comment.getParentCommentId() == null) {
            return isVisiblePost(comment);
        }

        return !POST_STATUS_PENDING.equals(normalizedStatus);
    }

    private boolean isVisiblePost(CircleComment post) {
        if (post == null || post.getParentCommentId() != null) {
            return false;
        }

        if (!POST_STATUS_PUBLISHED.equals(normalizePostStatus(post.getStatus()))) {
            return false;
        }

        Circle circle = post.getCircle();
        if (circle == null && post.getCircleId() != null) {
            circle = circleService.getCircleById(post.getCircleId()).orElse(null);
        }
        return isApprovedPublicCircle(circle);
    }

    private boolean isApprovedPublicCircle(Circle circle) {
        return circle != null
                && "approved".equalsIgnoreCase(circle.getType())
                && "public".equalsIgnoreCase(circle.getVisibility());
    }

    private Optional<CircleComment> resolveRootPost(CircleComment comment) {
        if (comment == null) {
            return Optional.empty();
        }

        if (comment.getParentCommentId() == null) {
            return Optional.of(comment);
        }

        Long rootPostId = resolveRootPostId(comment);
        return circleCommentService.getCommentById(rootPostId)
                .filter(root -> root.getParentCommentId() == null);
    }

    private Long resolveRootPostId(CircleComment comment) {
        return comment.getPostId() != null ? comment.getPostId() : comment.getId();
    }

    private String normalizePostStatus(String status) {
        if (status == null || status.trim().isEmpty()) {
            return POST_STATUS_PENDING;
        }

        String normalized = status.trim().toLowerCase();
        if ("approved".equals(normalized) || "normal".equals(normalized)) {
            return POST_STATUS_PUBLISHED;
        }
        if ("close".equals(normalized) || "closed".equals(normalized)) {
            return POST_STATUS_DELETED;
        }
        return normalized;
    }

    private Long requireCurrentUserId() {
        try {
            return UserUtil.getCurrentUserId();
        } catch (RuntimeException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "用户未登录");
        }
    }

    private Long tryGetCurrentUserId() {
        try {
            return UserUtil.getCurrentUserId();
        } catch (RuntimeException e) {
            return null;
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
