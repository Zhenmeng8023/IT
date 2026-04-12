package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmoduleinteractive.dto.CountResponseDTO;
import com.alikeyou.itmoduleinteractive.dto.UserActivityHeatmapResponseDTO;
import com.alikeyou.itmoduleinteractive.service.UserActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
public class UserActivityController {

    @Autowired
    private UserActivityService userActivityService;

    @Operation(summary = "获取用户获赞总数", description = "统计用户博客、圈子发帖/回复和博客评论累计获得的点赞总数")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户获赞总数",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponseDTO.class)))
    })
    @GetMapping("/{userId}/stats/likes")
    public ResponseEntity<CountResponseDTO> getUserLikes(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        long count = userActivityService.getUserReceivedLikes(userId);
        return ResponseEntity.ok(new CountResponseDTO(userId, "likes", count));
    }

    @Operation(summary = "获取用户收藏总数", description = "统计用户累计收藏的内容数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户收藏总数",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponseDTO.class)))
    })
    @GetMapping("/{userId}/stats/collects")
    public ResponseEntity<CountResponseDTO> getUserCollects(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        long count = userActivityService.getUserCollectCount(userId);
        return ResponseEntity.ok(new CountResponseDTO(userId, "collects", count));
    }

    @Operation(summary = "获取用户浏览记录总数", description = "统计用户累计浏览历史数量")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户浏览记录总数",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = CountResponseDTO.class)))
    })
    @GetMapping("/{userId}/stats/history")
    public ResponseEntity<CountResponseDTO> getUserHistoryCount(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId) {
        long count = userActivityService.getUserHistoryCount(userId);
        return ResponseEntity.ok(new CountResponseDTO(userId, "history", count));
    }

    @Operation(summary = "获取用户活动热力图", description = "聚合最近一段时间的发文、评论、收藏、点赞和浏览等用户活动，用于前端热力图展示")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功获取用户活动热力图",
                    content = @Content(mediaType = "application/json", schema = @Schema(implementation = UserActivityHeatmapResponseDTO.class)))
    })
    @GetMapping("/{userId}/activity-heatmap")
    public ResponseEntity<UserActivityHeatmapResponseDTO> getUserActivityHeatmap(
            @Parameter(description = "用户ID", required = true)
            @PathVariable Long userId,
            @Parameter(description = "统计天数，默认30，最大180")
            @RequestParam(required = false, defaultValue = "30") Integer days) {
        return ResponseEntity.ok(userActivityService.getUserActivityHeatmap(userId, days == null ? 30 : days));
    }
}
