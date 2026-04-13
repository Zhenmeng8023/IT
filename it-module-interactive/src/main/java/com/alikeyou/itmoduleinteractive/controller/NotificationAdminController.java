package com.alikeyou.itmoduleinteractive.controller;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.dto.NotificationDTO;
import com.alikeyou.itmoduleinteractive.dto.NotificationSystemRequest;
import com.alikeyou.itmoduleinteractive.dto.NotificationTemplateRequest;
import com.alikeyou.itmoduleinteractive.entity.NotificationTemplate;
import com.alikeyou.itmoduleinteractive.repository.NotificationTemplateRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@Tag(name = "管理员通知中心", description = "管理员消息通知管理、系统通知发送和模板管理")
public class NotificationAdminController {

    private final NotificationController notificationController;
    private final NotificationService notificationService;
    private final NotificationTemplateRepository templateRepository;
    private final UserInfoRepository userInfoRepository;

    @GetMapping("/notifications/page")
    public ResponseEntity<?> pageNotifications(@RequestParam(defaultValue = "1") int page,
                                               @RequestParam(defaultValue = "20") int size,
                                               @RequestParam(required = false) Long receiverId,
                                               @RequestParam(required = false) Long senderId,
                                               @RequestParam(required = false) String category,
                                               @RequestParam(required = false) String type,
                                               @RequestParam(required = false) Boolean readStatus,
                                               @RequestParam(required = false) String businessStatus,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant startTime,
                                               @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant endTime,
                                               Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        Page<NotificationDTO> result = notificationService.listAdminNotifications(receiverId, senderId, category, type,
                readStatus, businessStatus, startTime, endTime, page, size);
        return ResponseEntity.ok(notificationController.success(notificationController.pageData(result, page, size)));
    }

    @GetMapping("/notifications/{id}")
    public ResponseEntity<?> getNotification(@PathVariable Long id, Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        NotificationDTO dto = notificationService.getAdminNotification(id);
        return dto == null
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("通知不存在"))
                : ResponseEntity.ok(notificationController.success(dto));
    }

    @DeleteMapping("/notifications/{id}")
    public ResponseEntity<?> deleteNotification(@PathVariable Long id, Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        if (!notificationService.adminDeleteNotification(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("通知不存在"));
        }
        return ResponseEntity.ok(notificationController.successMessage("通知已删除"));
    }

    @PutMapping("/notifications/{id}/read")
    public ResponseEntity<?> markRead(@PathVariable Long id, Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        NotificationDTO dto = notificationService.adminMarkAsRead(id);
        return dto == null
                ? ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("通知不存在"))
                : ResponseEntity.ok(notificationController.success(dto));
    }

    @PutMapping("/notifications/read-all")
    public ResponseEntity<?> batchMarkRead(@RequestBody(required = false) Map<String, List<Long>> body,
                                           Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        int count = notificationService.adminBatchMarkAsRead(body == null ? List.of() : body.get("ids"));
        return ResponseEntity.ok(notificationController.success(Map.of("updatedCount", count)));
    }

    @DeleteMapping("/notifications/batch")
    public ResponseEntity<?> batchDelete(@RequestBody(required = false) Map<String, List<Long>> body,
                                         Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        int count = notificationService.adminBatchDeleteNotifications(body == null ? List.of() : body.get("ids"));
        return ResponseEntity.ok(notificationController.success(Map.of("deletedCount", count)));
    }

    @PostMapping("/notifications/system")
    public ResponseEntity<?> publishSystemNotification(@RequestBody NotificationSystemRequest request,
                                                       Authentication authentication) {
        return sendSystem(request, authentication);
    }

    @PostMapping("/notifications/broadcast")
    public ResponseEntity<?> publishBroadcastNotification(@RequestBody NotificationSystemRequest request,
                                                          Authentication authentication) {
        if (request != null && !StringUtils.hasText(request.getSendScope())) {
            request.setSendScope("all");
        }
        return sendSystem(request, authentication);
    }

    @GetMapping("/notifications/stats")
    public ResponseEntity<?> stats(Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        return ResponseEntity.ok(notificationController.success(notificationService.adminStats()));
    }

    @GetMapping("/notifications/users/search")
    public ResponseEntity<?> searchUsers(@RequestParam(required = false) String keyword,
                                         @RequestParam(defaultValue = "10") int size,
                                         Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        List<Map<String, Object>> users = userInfoRepository.searchForNotification(keyword, PageRequest.of(0, Math.min(Math.max(size, 1), 50)))
                .stream()
                .map(this::userOption)
                .toList();
        return ResponseEntity.ok(notificationController.success(users));
    }

    @GetMapping("/notification-templates/page")
    public ResponseEntity<?> pageTemplates(@RequestParam(defaultValue = "1") int page,
                                           @RequestParam(defaultValue = "20") int size,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String category,
                                           @RequestParam(required = false) String type,
                                           @RequestParam(required = false) Boolean enabled,
                                           Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        Page<NotificationTemplate> result = templateRepository.pageTemplates(keyword, category, type, enabled,
                PageRequest.of(Math.max(page - 1, 0), Math.min(Math.max(size, 1), 100), Sort.by(Sort.Direction.DESC, "updatedAt")));
        return ResponseEntity.ok(notificationController.success(notificationController.pageData(result.map(this::templateMap), page, size)));
    }

    @GetMapping("/notification-templates/{id}")
    public ResponseEntity<?> getTemplate(@PathVariable Long id, Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        return templateRepository.findById(id)
                .filter(template -> template.getDeletedAt() == null)
                .map(template -> ResponseEntity.ok(notificationController.success(templateMap(template))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("模板不存在")));
    }

    @PostMapping("/notification-templates")
    public ResponseEntity<?> createTemplate(@RequestBody NotificationTemplateRequest request,
                                            Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        String error = validateTemplate(request);
        if (error != null) {
            return ResponseEntity.badRequest().body(notificationController.error(error));
        }
        if (templateRepository.findByCodeAndDeletedAtIsNull(request.getCode().trim()).isPresent()) {
            return ResponseEntity.badRequest().body(notificationController.error("模板编码已存在"));
        }
        NotificationTemplate template = applyTemplate(new NotificationTemplate(), request);
        template.setCreatedAt(Instant.now());
        return ResponseEntity.ok(notificationController.success(templateMap(templateRepository.save(template))));
    }

    @PutMapping("/notification-templates/{id}")
    public ResponseEntity<?> updateTemplate(@PathVariable Long id,
                                            @RequestBody NotificationTemplateRequest request,
                                            Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        String error = validateTemplate(request);
        if (error != null) {
            return ResponseEntity.badRequest().body(notificationController.error(error));
        }
        return templateRepository.findById(id)
                .filter(template -> template.getDeletedAt() == null)
                .map(template -> ResponseEntity.ok(notificationController.success(templateMap(templateRepository.save(applyTemplate(template, request))))))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("模板不存在")));
    }

    @PutMapping("/notification-templates/{id}/enabled")
    public ResponseEntity<?> updateTemplateEnabled(@PathVariable Long id,
                                                   @RequestBody Map<String, Boolean> body,
                                                   Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        return templateRepository.findById(id)
                .filter(template -> template.getDeletedAt() == null)
                .map(template -> {
                    template.setEnabled(Boolean.TRUE.equals(body == null ? null : body.get("enabled")));
                    template.setUpdatedAt(Instant.now());
                    return ResponseEntity.ok(notificationController.success(templateMap(templateRepository.save(template))));
                })
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("模板不存在")));
    }

    @DeleteMapping("/notification-templates/{id}")
    public ResponseEntity<?> deleteTemplate(@PathVariable Long id, Authentication authentication) {
        if (!ensureAdmin(authentication)) {
            return forbidden();
        }
        if (templateRepository.softDeleteById(id) <= 0) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(notificationController.error("模板不存在"));
        }
        return ResponseEntity.ok(notificationController.successMessage("模板已删除"));
    }

    private ResponseEntity<?> sendSystem(NotificationSystemRequest request, Authentication authentication) {
        UserInfo admin = notificationController.resolveCurrentUser(authentication);
        if (!notificationController.isAdmin(admin)) {
            return forbidden();
        }
        try {
            int count = notificationService.publishAdminSystemNotification(request, admin.getId());
            return ResponseEntity.ok(notificationController.success(Map.of("sentCount", count)));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(notificationController.error(ex.getMessage()));
        }
    }

    private boolean ensureAdmin(Authentication authentication) {
        return notificationController.isAdmin(notificationController.resolveCurrentUser(authentication));
    }

    private ResponseEntity<Map<String, Object>> forbidden() {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(notificationController.error("仅管理员可访问"));
    }

    private NotificationTemplate applyTemplate(NotificationTemplate template, NotificationTemplateRequest request) {
        Instant now = Instant.now();
        template.setCode(request.getCode().trim());
        template.setCategory(defaultText(request.getCategory(), "system"));
        template.setType(defaultText(request.getType(), "system"));
        template.setTitleTemplate(request.getTitleTemplate().trim());
        template.setContentTemplate(request.getContentTemplate().trim());
        template.setActionUrlTemplate(blankToNull(request.getActionUrlTemplate()));
        template.setDefaultPriority(request.getDefaultPriority() == null ? 0 : request.getDefaultPriority());
        template.setEnabled(request.getEnabled() == null || request.getEnabled());
        template.setRemark(blankToNull(request.getRemark()));
        template.setUpdatedAt(now);
        return template;
    }

    private String validateTemplate(NotificationTemplateRequest request) {
        if (request == null || !StringUtils.hasText(request.getCode())) {
            return "模板编码不能为空";
        }
        if (!StringUtils.hasText(request.getTitleTemplate())) {
            return "标题模板不能为空";
        }
        if (!StringUtils.hasText(request.getContentTemplate())) {
            return "内容模板不能为空";
        }
        return null;
    }

    private Map<String, Object> templateMap(NotificationTemplate template) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", template.getId());
        data.put("code", template.getCode());
        data.put("category", template.getCategory());
        data.put("type", template.getType());
        data.put("titleTemplate", template.getTitleTemplate());
        data.put("contentTemplate", template.getContentTemplate());
        data.put("actionUrlTemplate", template.getActionUrlTemplate());
        data.put("defaultPriority", template.getDefaultPriority());
        data.put("enabled", template.getEnabled());
        data.put("remark", template.getRemark());
        data.put("createdAt", template.getCreatedAt());
        data.put("updatedAt", template.getUpdatedAt());
        return data;
    }

    private Map<String, Object> userOption(UserInfo user) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", user.getId());
        data.put("username", user.getUsername());
        data.put("nickname", user.getNickname());
        data.put("avatarUrl", user.getAvatarUrl());
        data.put("roleId", user.getRoleId());
        return data;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
