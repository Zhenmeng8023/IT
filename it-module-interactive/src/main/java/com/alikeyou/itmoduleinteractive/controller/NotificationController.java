package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.entity.Comment;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.repository.CommentRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/notifications")
@Tag(name = "通知管理", description = "用户消息通知中心（通用）")
public class NotificationController {

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private BlogRepository blogRepository;

    @Autowired
    private CommentRepository commentRepository;

    @GetMapping({"", "/my"})
    @Operation(summary = "获取我的通知", description = "获取当前用户的所有通知")
    public ResponseEntity<?> getUserNotifications(Authentication authentication) {
        try {
            UserInfo currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<Notification> notifications = notificationService.getUserNotifications(currentUser.getId());
            enrichNotifications(notifications);
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
            UserInfo currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            List<Notification> notifications = notificationService.getUserUnreadNotifications(currentUser.getId());
            enrichNotifications(notifications);
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
            UserInfo currentUser = resolveCurrentUser(authentication);
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
            UserInfo currentUser = resolveCurrentUser(authentication);
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

    @PutMapping("/read-all")
    @Operation(summary = "标记全部通知为已读", description = "将当前用户的所有未读通知标记为已读")
    public ResponseEntity<?> markAllNotificationsAsRead(Authentication authentication) {
        try {
            UserInfo currentUser = resolveCurrentUser(authentication);
            if (currentUser == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("用户未登录");
            }

            long updatedCount = notificationService.markAllAsRead(currentUser.getId());
            Map<String, Long> response = new HashMap<>();
            response.put("updatedCount", updatedCount);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("批量标记已读失败：" + e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除通知", description = "删除指定通知")
    public ResponseEntity<?> deleteNotification(
            @Parameter(description = "通知 ID") @PathVariable Long id,
            Authentication authentication) {
        try {
            UserInfo currentUser = resolveCurrentUser(authentication);
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
            UserInfo currentUser = resolveCurrentUser(authentication);
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

    private UserInfo resolveCurrentUser(Authentication authentication) {
        try {
            UserInfo user = UserUtil.getCurrentUser(authentication);
            return user != null && user.getId() != null ? user : null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private void enrichNotifications(List<Notification> notifications) {
        if (notifications == null || notifications.isEmpty()) {
            return;
        }

        Map<Long, UserInfo> senderMap = loadSenders(notifications.stream()
                .map(Notification::getSenderId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, Comment> commentMap = loadComments(notifications.stream()
                .filter(notification -> "comment".equalsIgnoreCase(notification.getTargetType()))
                .map(Notification::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Set<Long> blogIds = notifications.stream()
                .filter(notification -> "blog".equalsIgnoreCase(notification.getTargetType()))
                .map(Notification::getTargetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        blogIds.addAll(commentMap.values().stream()
                .map(Comment::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));

        Map<Long, Blog> blogMap = loadBlogs(blogIds);

        notifications.forEach(notification -> {
            UserInfo sender = senderMap.get(notification.getSenderId());
            notification.setSenderName(getDisplayName(sender));
            notification.setSenderAvatar(sender != null ? sender.getAvatarUrl() : null);
            notification.setPreview(notification.getContent());
            notification.setActionText(resolveActionText(notification.getType()));

            if ("comment".equalsIgnoreCase(notification.getTargetType()) && notification.getTargetId() != null) {
                Comment comment = commentMap.get(notification.getTargetId());
                if (comment != null) {
                    notification.setCommentId(comment.getId());
                    notification.setBlogId(comment.getPostId());
                    Blog blog = blogMap.get(comment.getPostId());
                    notification.setTargetTitle(blog != null ? blog.getTitle() : "相关博客");
                } else {
                    notification.setCommentId(notification.getTargetId());
                }
            } else if ("blog".equalsIgnoreCase(notification.getTargetType()) && notification.getTargetId() != null) {
                notification.setBlogId(notification.getTargetId());
                Blog blog = blogMap.get(notification.getTargetId());
                notification.setTargetTitle(blog != null ? blog.getTitle() : "相关博客");
            }
        });
    }

    private Map<Long, UserInfo> loadSenders(Collection<Long> senderIds) {
        if (senderIds == null || senderIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, UserInfo> senderMap = new HashMap<>();
        userInfoRepository.findAllById(senderIds).forEach(sender -> senderMap.put(sender.getId(), sender));
        return senderMap;
    }

    private Map<Long, Comment> loadComments(Collection<Long> commentIds) {
        if (commentIds == null || commentIds.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<Long, Comment> commentMap = new HashMap<>();
        commentRepository.findAllById(commentIds).forEach(comment -> commentMap.put(comment.getId(), comment));
        return commentMap;
    }

    private Map<Long, Blog> loadBlogs(Collection<Long> blogIds) {
        if (blogIds == null || blogIds.isEmpty()) {
            return Collections.emptyMap();
        }

        return blogRepository.findByIdIn(blogIds.stream().toList()).stream()
                .collect(Collectors.toMap(Blog::getId, blog -> blog));
    }

    private String getDisplayName(UserInfo sender) {
        if (sender == null) {
            return "系统";
        }
        if (sender.getNickname() != null && !sender.getNickname().isBlank()) {
            return sender.getNickname();
        }
        if (sender.getUsername() != null && !sender.getUsername().isBlank()) {
            return sender.getUsername();
        }
        return "匿名用户";
    }

    private String resolveActionText(String type) {
        if (type == null) {
            return "给你带来了新动态";
        }
        return switch (type.toLowerCase()) {
            case "reply" -> "回复了你的评论";
            case "comment" -> "评论了你的博客";
            case "like" -> "点赞了你的内容";
            case "system" -> "发送了一条系统通知";
            default -> "给你带来了新动态";
        };
    }
}
