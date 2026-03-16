package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.LikeRecord;
import com.alikeyou.itmoduleinteractive.entity.CollectRecord;
import com.alikeyou.itmoduleinteractive.service.CommentService;
import com.alikeyou.itmoduleinteractive.service.LikeRecordService;
import com.alikeyou.itmoduleinteractive.service.CollectRecordService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import org.springframework.security.core.Authentication;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/blogs")
public class InteractiveController {

    @Autowired
    private CommentService commentService;

    @Autowired
    private LikeRecordService likeRecordService;

    @Autowired
    private CollectRecordService collectRecordService;

    // 获取所有评论(后台使用)
    @Operation(summary = "获取所有评论", description = "获取表中的所有评论信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取评论列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class)))
    })
    @GetMapping("/comments")
    public ResponseEntity<List<Comment>> getAllComments() {
        List<Comment> comments = commentService.getAllComments();
        return ResponseEntity.ok(comments);
    }

    // 添加评论
    @Operation(summary = "添加评论", description = "添加新评论，需要提供除自动生成字段外的所有信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功添加评论",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class)))
    })
    @PostMapping("/comments")
    public ResponseEntity<Comment> addComment(@Parameter(description = "评论信息") @RequestBody Comment comment) {
        Comment savedComment = commentService.saveComment(comment);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedComment);
    }

    // 根据ID获取评论
    @Operation(summary = "根据ID获取评论", description = "根据评论ID获取评论详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取评论信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class))),
            @ApiResponse(responseCode = "404", description = "评论不存在")
    })
    @GetMapping("/comments/{id}")
    public ResponseEntity<Comment> getCommentById(@Parameter(description = "评论ID") @PathVariable Long id) {
        return commentService.getCommentById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 获取某一博客下的所有评论
    @Operation(summary = "获取某一博客下的所有评论", description = "根据博客ID获取该博客下的所有评论")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取评论列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = Comment.class)))
    })
    @GetMapping("/comments/post/{postId}")
    public ResponseEntity<List<Comment>> getCommentsByPostId(@Parameter(description = "博客ID") @PathVariable Long postId) {
        List<Comment> comments = commentService.getCommentsByPostId(postId);
        return ResponseEntity.ok(comments);
    }

    // 点赞记录相关接口
    // 获取所有点赞记录
    @Operation(summary = "获取所有点赞记录", description = "获取表中的所有点赞记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取点赞记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class)))
    })
    @GetMapping("/likes")
    public ResponseEntity<List<LikeRecord>> getAllLikeRecords() {
        List<LikeRecord> likeRecords = likeRecordService.getAllLikeRecords();
        return ResponseEntity.ok(likeRecords);
    }

    // 添加点赞记录
    @Operation(summary = "添加点赞记录", description = "添加新点赞记录，需要提供用户ID、目标类型和目标ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功添加点赞记录",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class)))
    })
    @PostMapping("/likes")
    public ResponseEntity<LikeRecord> addLikeRecord(@Parameter(description = "点赞记录信息") @RequestBody LikeRecord likeRecord) {
        LikeRecord savedLikeRecord = likeRecordService.createLikeRecord(likeRecord);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedLikeRecord);
    }

    // 删除点赞记录
    @Operation(summary = "删除点赞记录", description = "根据点赞记录ID删除点赞记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除点赞记录")
    })
    @DeleteMapping("/likes/{id}")
    public ResponseEntity<Void> deleteLikeRecord(@Parameter(description = "点赞记录ID") @PathVariable Long id) {
        likeRecordService.deleteLikeRecord(id);
        return ResponseEntity.noContent().build();
    }

    // 根据ID获取点赞记录
    @Operation(summary = "根据ID获取点赞记录", description = "根据点赞记录ID获取点赞记录详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取点赞记录信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class))),
            @ApiResponse(responseCode = "404", description = "点赞记录不存在")
    })
    @GetMapping("/likes/{id}")
    public ResponseEntity<LikeRecord> getLikeRecordById(@Parameter(description = "点赞记录ID") @PathVariable Long id) {
        return likeRecordService.getLikeRecordById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 根据用户ID、目标类型和目标ID获取点赞记录
    @Operation(summary = "根据用户ID、目标类型和目标ID获取点赞记录", description = "检查用户是否已经对某目标点过赞")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取点赞记录信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class))),
            @ApiResponse(responseCode = "404", description = "点赞记录不存在")
    })
    @GetMapping("/likes/user/{userId}/target/{targetType}/{targetId}")
    public ResponseEntity<LikeRecord> getLikeRecordByUserAndTarget(@Parameter(description = "用户ID") @PathVariable Long userId,
                                                                 @Parameter(description = "目标类型") @PathVariable String targetType,
                                                                 @Parameter(description = "目标ID") @PathVariable Long targetId) {
        return likeRecordService.getLikeRecordByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 根据目标类型和目标ID获取点赞记录列表
    @Operation(summary = "根据目标类型和目标ID获取点赞记录列表", description = "获取某一目标的所有点赞记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取点赞记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class)))
    })
    @GetMapping("/likes/target/{targetType}/{targetId}")
    public ResponseEntity<List<LikeRecord>> getLikeRecordsByTarget(@Parameter(description = "目标类型") @PathVariable String targetType,
                                                                  @Parameter(description = "目标ID") @PathVariable Long targetId) {
        List<LikeRecord> likeRecords = likeRecordService.getLikeRecordsByTargetTypeAndTargetId(targetType, targetId);
        return ResponseEntity.ok(likeRecords);
    }

    // 根据用户ID获取点赞记录列表
    @Operation(summary = "根据用户ID获取点赞记录列表", description = "获取某一用户的所有点赞记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取点赞记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = LikeRecord.class)))
    })
    @GetMapping("/likes/user/{userId}")
    public ResponseEntity<List<LikeRecord>> getLikeRecordsByUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<LikeRecord> likeRecords = likeRecordService.getLikeRecordsByUserId(userId);
        return ResponseEntity.ok(likeRecords);
    }

    // 收藏相关接口
    // 获取所有收藏记录
    @Operation(summary = "获取所有收藏记录", description = "获取表中的所有收藏记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取收藏记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class)))
    })
    @GetMapping("/collects")
    public ResponseEntity<List<CollectRecord>> getAllCollectRecords() {
        List<CollectRecord> collectRecords = collectRecordService.getAllCollectRecords();
        return ResponseEntity.ok(collectRecords);
    }

    // 添加收藏记录
    @Operation(summary = "添加收藏记录", description = "添加新收藏记录，需要提供用户ID、目标类型和目标ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "成功添加收藏记录",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class))),
            @ApiResponse(responseCode = "400", description = "已经收藏过该目标")
    })
    @PostMapping("/collects")
    public ResponseEntity<CollectRecord> addCollectRecord(@Parameter(description = "收藏记录信息") @RequestBody CollectRecord collectRecord) {
        try {
            CollectRecord savedCollectRecord = collectRecordService.createCollectRecord(collectRecord);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedCollectRecord);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
        }
    }

    // 删除收藏记录
    @Operation(summary = "删除收藏记录", description = "根据收藏记录ID删除收藏记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "成功删除收藏记录")
    })
    @DeleteMapping("/collects/{id}")
    public ResponseEntity<Void> deleteCollectRecord(@Parameter(description = "收藏记录ID") @PathVariable Long id) {
        collectRecordService.deleteCollectRecord(id);
        return ResponseEntity.noContent().build();
    }

    // 根据ID获取收藏记录
    @Operation(summary = "根据ID获取收藏记录", description = "根据收藏记录ID获取收藏记录详细信息")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取收藏记录信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class))),
            @ApiResponse(responseCode = "404", description = "收藏记录不存在")
    })
    @GetMapping("/collects/{id}")
    public ResponseEntity<CollectRecord> getCollectRecordById(@Parameter(description = "收藏记录ID") @PathVariable Long id) {
        return collectRecordService.getCollectRecordById(id)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 根据用户ID、目标类型和目标ID获取收藏记录
    @Operation(summary = "根据用户ID、目标类型和目标ID获取收藏记录", description = "检查用户是否已经对某目标收藏过")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取收藏记录信息",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class))),
            @ApiResponse(responseCode = "404", description = "收藏记录不存在")
    })
    @GetMapping("/collects/user/{userId}/target/{targetType}/{targetId}")
    public ResponseEntity<CollectRecord> getCollectRecordByUserAndTarget(@Parameter(description = "用户ID") @PathVariable Long userId,
                                                                     @Parameter(description = "目标类型") @PathVariable String targetType,
                                                                     @Parameter(description = "目标ID") @PathVariable Long targetId) {
        return collectRecordService.getCollectRecordByUserIdAndTargetTypeAndTargetId(userId, targetType, targetId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).build());
    }

    // 根据目标类型和目标ID获取收藏记录列表
    @Operation(summary = "根据目标类型和目标ID获取收藏记录列表", description = "获取某一目标的所有收藏记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取收藏记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class)))
    })
    @GetMapping("/collects/target/{targetType}/{targetId}")
    public ResponseEntity<List<CollectRecord>> getCollectRecordsByTarget(@Parameter(description = "目标类型") @PathVariable String targetType,
                                                                      @Parameter(description = "目标ID") @PathVariable Long targetId) {
        List<CollectRecord> collectRecords = collectRecordService.getCollectRecordsByTargetTypeAndTargetId(targetType, targetId);
        return ResponseEntity.ok(collectRecords);
    }

    // 根据用户ID获取收藏记录列表
    @Operation(summary = "根据用户ID获取收藏记录列表", description = "获取某一用户的所有收藏记录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取收藏记录列表",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CollectRecord.class)))
    })
    @GetMapping("/collects/user/{userId}")
    public ResponseEntity<List<CollectRecord>> getCollectRecordsByUser(@Parameter(description = "用户ID") @PathVariable Long userId) {
        List<CollectRecord> collectRecords = collectRecordService.getCollectRecordsByUserId(userId);
        return ResponseEntity.ok(collectRecords);
    }

    // 检查是否已收藏
    @Operation(summary = "检查是否已收藏", description = "检查当前用户是否已收藏指定博客")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功检查收藏状态")
    })
    @GetMapping("/{blogId}/is-collected")
    public ResponseEntity<Boolean> isCollected(@Parameter(description = "博客ID") @PathVariable Long blogId, Authentication authentication) {
        UserInfo user = UserUtil.getCurrentUser(authentication);
        boolean isCollected = collectRecordService.isCollected(user, "blog", blogId);
        return ResponseEntity.ok(isCollected);
    }
}