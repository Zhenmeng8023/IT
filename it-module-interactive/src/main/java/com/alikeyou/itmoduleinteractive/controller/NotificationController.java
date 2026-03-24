package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "用户消息通知中心（通用）")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @GetMapping
    @Operation(summary = "获取我的通知", description = "获取当前用户的所有通知")
    public ResponseEntity<?> getUserNotifications(Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/unread")
    @Operation(summary = "获取未读通知", description = "获取当前用户的未读通知")
    public ResponseEntity<?> getUserUnreadNotifications(Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<Notification> notifications = notificationService.getUserUnreadNotifications(currentUser.getId());
            return ResponseEntity.ok(notifications);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取未读通知失败：" + e.getMessage());
        }
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数", description = "获取当前用户的未读通知数量")
    public ResponseEntity<?> getUnreadNotificationCount(Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            long count = notificationService.countUserUnreadNotifications(currentUser.getId());
            Map<String, Long> response = new HashMap<>();
            response.put("count", count);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("获取未读数失败：" + e.getMessage());
        }
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记通知为已读", description = "将指定通知标记为已读")
    public ResponseEntity<?> markNotificationAsRead(
            @Parameter(description = "通知 ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            Notification updated = notificationService.markAsRead(id, currentUser.getId());
            if (updated != null) {
                return ResponseEntity.ok(updated);
            } else {
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("标记已读失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public ResponseEntity<?> deleteNotification(
            @Parameter(description = "通知 ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            notificationService.deleteNotification(id, currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "通知删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("删除通知失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/clear")
    @Operation(summary = "清空通知", description = "清空当前用户的所有通知")
    public ResponseEntity<?> clearUserNotifications(Authentication authentication) {
        try {
            UserInfo currentUser = UserUtil.getCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            notificationService.clearUserNotifications(currentUser.getId());
            Map<String, String> response = new HashMap<>();
            response.put("message", "通知清空成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("清空通知失败：" + e.getMessage());
        }
    }
}
