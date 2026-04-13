package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulecommon.utils.UserUtil;
import com.alikeyou.itmoduleinteractive.dto.NotificationDTO;
import com.alikeyou.itmoduleinteractive.dto.NotificationSystemRequest;
import com.alikeyou.itmoduleinteractive.dto.NotificationUnreadCountResponse;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "通知中心", description = "用户站内消息通知中心")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserInfoRepository userInfoRepository;

    @GetMapping({"", "/my"})
    @Operation(summary = "获取我的通知")
    public ResponseEntity<?> getMyNotifications(@RequestParam(defaultValue = "1") int page,
                                                @RequestParam(defaultValue = "20") int size,
                                                @RequestParam(required = false) String category,
                                                @RequestParam(required = false) String type,
                                                @RequestParam(required = false) Boolean readStatus,
                                                Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        Page<NotificationDTO> notificationPage = notificationService.listMyNotifications(
                currentUser.getId(), page, size, normalizeCategory(category), blankToNull(type), readStatus);
        return ResponseEntity.ok(success(pageData(notificationPage, page, size)));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "获取未读通知数量")
    public ResponseEntity<?> getUnreadNotificationCount(Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(success(new NotificationUnreadCountResponse(notificationService.countUnread(currentUser.getId()))));
    }

    @GetMapping("/unread-counts")
    @Operation(summary = "按分类获取未读通知数量")
    public ResponseEntity<?> getUnreadNotificationCounts(Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        return ResponseEntity.ok(success(notificationService.countUnreadByCategory(currentUser.getId())));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "标记单条通知为已读")
    public ResponseEntity<?> markNotificationAsRead(@PathVariable Long id, Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        NotificationDTO updated = notificationService.markAsRead(id, currentUser.getId());
        if (updated == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("通知不存在或无权操作"));
        }
        return ResponseEntity.ok(success(updated));
    }

    @PutMapping("/read-all")
    @Operation(summary = "批量标记通知为已读")
    public ResponseEntity<?> markAllNotificationsAsRead(@RequestParam(required = false) String category,
                                                        Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        Map<String, Long> data = new HashMap<>();
        data.put("updatedCount", notificationService.markAllAsRead(currentUser.getId(), normalizeCategory(category)));
        return ResponseEntity.ok(success(data));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "软删除单条通知")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        if (!notificationService.deleteNotification(id, currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(error("通知不存在或无权操作"));
        }
        return ResponseEntity.ok(successMessage("通知已删除"));
    }

    @DeleteMapping("/clear")
    @Operation(summary = "软清空我的通知")
    public ResponseEntity<?> clearUserNotifications(Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (currentUser == null) {
            return unauthorized();
        }
        Map<String, Long> data = new HashMap<>();
        data.put("deletedCount", notificationService.clearUserNotifications(currentUser.getId()));
        return ResponseEntity.ok(success(data));
    }

    @PostMapping("/system")
    @Operation(summary = "管理员发送单个系统通知")
    public ResponseEntity<?> publishSystemNotification(@RequestBody NotificationSystemRequest request,
                                                       Authentication authentication) {
        UserInfo currentUser = resolveCurrentUser(authentication);
        if (!isAdmin(currentUser)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(error("仅管理员可发送系统通知"));
        }
        if (request == null || request.getReceiverId() == null) {
            return ResponseEntity.badRequest().body(error("receiverId 不能为空"));
        }
        notificationService.publishSystemNotification(request.getReceiverId(), request.getTitle(), request.getContent(),
                request.getActionUrl(), currentUser.getId());
        return ResponseEntity.ok(successMessage("系统通知已发送"));
    }

    protected UserInfo resolveCurrentUser(Authentication authentication) {
        try {
            UserInfo user = UserUtil.getCurrentUser(authentication);
            if (user == null) {
                return null;
            }
            if (user.getId() != null) {
                return userInfoRepository.findById(user.getId()).orElse(user);
            }
            if (StringUtils.hasText(user.getUsername())) {
                return userInfoRepository.findByUsername(user.getUsername()).orElse(null);
            }
            return null;
        } catch (RuntimeException ex) {
            return null;
        }
    }

    protected boolean isAdmin(UserInfo user) {
        return user != null && user.getRoleId() != null && (user.getRoleId() == 1 || user.getRoleId() == 2);
    }

    protected String normalizeCategory(String category) {
        if (!StringUtils.hasText(category) || "all".equalsIgnoreCase(category.trim())) {
            return null;
        }
        return category.trim();
    }

    protected String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    protected Map<String, Object> pageData(Page<?> pageData, int page, int size) {
        Map<String, Object> data = new HashMap<>();
        data.put("list", pageData.getContent());
        data.put("total", pageData.getTotalElements());
        data.put("page", Math.max(page, 1));
        data.put("size", Math.max(size, 1));
        data.put("pages", pageData.getTotalPages());
        return data;
    }

    protected ResponseEntity<Map<String, Object>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error("用户未登录"));
    }

    protected Map<String, Object> success(Object data) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("code", 200);
        response.put("data", data);
        return response;
    }

    protected Map<String, Object> successMessage(String message) {
        Map<String, Object> response = success(null);
        response.put("message", message);
        return response;
    }

    protected Map<String, Object> error(String message) {
        Map<String, Object> response = new HashMap<>();
        response.put("success", false);
        response.put("code", 500);
        response.put("message", message);
        return response;
    }
}
