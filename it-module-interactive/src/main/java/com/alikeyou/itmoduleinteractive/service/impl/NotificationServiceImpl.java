package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.notification.NotificationBusinessStatusUpdateEvent;
import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmodulecommon.notification.NotificationPublishEvent;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.dto.NotificationDTO;
import com.alikeyou.itmoduleinteractive.dto.NotificationSystemRequest;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.entity.NotificationDelivery;
import com.alikeyou.itmoduleinteractive.entity.NotificationEventLog;
import com.alikeyou.itmoduleinteractive.repository.NotificationDeliveryRepository;
import com.alikeyou.itmoduleinteractive.repository.NotificationEventLogRepository;
import com.alikeyou.itmoduleinteractive.repository.NotificationRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import com.alikeyou.itmoduleproject.entity.Project;
import com.alikeyou.itmoduleproject.repository.ProjectRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.event.EventListener;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private static final int MAX_PAGE_SIZE = 100;

    private final NotificationRepository notificationRepository;
    private final NotificationDeliveryRepository notificationDeliveryRepository;
    private final NotificationEventLogRepository notificationEventLogRepository;
    private final UserInfoRepository userInfoRepository;
    private final BlogRepository blogRepository;
    private final ProjectRepository projectRepository;
    private final ObjectMapper objectMapper;

    @EventListener
    @Transactional
    public void handleNotificationPublishEvent(NotificationPublishEvent event) {
        if (event != null && event.getCommand() != null) {
            createNotification(event.getCommand());
        }
    }

    @EventListener
    @Transactional
    public void handleNotificationBusinessStatusUpdateEvent(NotificationBusinessStatusUpdateEvent event) {
        if (event != null) {
            updateBusinessStatus(event.getSourceType(), event.getSourceId(), event.getBusinessStatus());
        }
    }

    @Override
    @Transactional
    public Notification createNotification(NotificationCreateCommand command) {
        if (command == null) {
            return null;
        }
        List<Long> receiverIds = resolveReceiverIds(command);
        if (receiverIds.isEmpty()) {
            writeEventLog(command, safeEventKey(command, null), 0, "skipped", "receiver is empty");
            return null;
        }

        Notification firstCreated = null;
        for (Long receiverId : receiverIds) {
            Notification created = createNotificationForReceiver(command, receiverId, receiverIds.size() > 1);
            if (created != null && firstCreated == null) {
                firstCreated = created;
            }
        }
        return firstCreated;
    }

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        if (notification == null) {
            return null;
        }
        return createNotification(NotificationCreateCommand.builder()
                .receiverId(notification.getReceiverId())
                .senderId(notification.getSenderId())
                .category(notification.getCategory())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .eventKey(notification.getEventKey())
                .actionUrl(notification.getActionUrl())
                .businessStatus(notification.getBusinessStatus())
                .priority(notification.getPriority())
                .payloadJson(notification.getPayloadJson())
                .expiresAt(notification.getExpiresAt())
                .build());
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> listMyNotifications(Long userId, int page, int size, String category, String type, Boolean readStatus) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.pageMine(userId, blankToNull(category), blankToNull(type), readStatus, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByReceiverIdAndReadStatusFalseAndDeletedAtIsNull(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> countUnreadByCategory(Long userId) {
        Map<String, Long> result = new LinkedHashMap<>();
        notificationRepository.countUnreadByCategory(userId).forEach(row -> {
            String category = row[0] == null ? "system" : String.valueOf(row[0]);
            long count = row[1] instanceof Number number ? number.longValue() : 0L;
            result.put(category, count);
        });
        result.put("all", result.values().stream().mapToLong(Long::longValue).sum());
        return result;
    }

    @Override
    @Transactional
    public NotificationDTO markAsRead(Long notificationId, Long userId) {
        if (notificationId == null || userId == null) {
            return null;
        }
        return notificationRepository.findById(notificationId)
                .filter(notification -> Objects.equals(notification.getReceiverId(), userId))
                .filter(notification -> notification.getDeletedAt() == null)
                .map(notification -> {
                    if (!Boolean.TRUE.equals(notification.getReadStatus())) {
                        Instant now = Instant.now();
                        notification.setReadStatus(Boolean.TRUE);
                        notification.setReadAt(now);
                        notification.setUpdatedAt(now);
                    }
                    return toDTO(notificationRepository.save(notification));
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public long markAllAsRead(Long userId, String category) {
        return userId == null ? 0 : notificationRepository.markAllAsRead(userId, blankToNull(category));
    }

    @Override
    @Transactional
    public boolean deleteNotification(Long notificationId, Long userId) {
        return notificationId != null && userId != null && notificationRepository.softDeleteByIdAndReceiverId(notificationId, userId) > 0;
    }

    @Override
    @Transactional
    public long clearUserNotifications(Long userId) {
        return userId == null ? 0 : notificationRepository.softDeleteByReceiverId(userId);
    }

    @Override
    @Transactional
    public void updateBusinessStatus(String sourceType, Long sourceId, String businessStatus) {
        if (StringUtils.hasText(sourceType) && sourceId != null && StringUtils.hasText(businessStatus)) {
            notificationRepository.updateBusinessStatusBySource(sourceType.trim(), sourceId, businessStatus.trim());
        }
    }

    @Override
    @Transactional
    public Notification publishSystemNotification(Long receiverId, String title, String content, String actionUrl, Long senderId) {
        return createNotification(NotificationCreateCommand.builder()
                .receiverId(receiverId)
                .senderId(senderId)
                .category("system")
                .type("system")
                .title(defaultText(title, "系统通知"))
                .content(defaultText(content, "你收到一条系统通知"))
                .targetType("system")
                .eventKey("system:" + receiverId + ":" + UUID.randomUUID())
                .actionUrl(actionUrl)
                .businessStatus("open")
                .priority(0)
                .build());
    }

    @Override
    @Transactional
    public int publishBroadcastNotification(Collection<Long> receiverIds, String title, String content, String actionUrl, Long senderId) {
        NotificationSystemRequest request = new NotificationSystemRequest();
        request.setReceiverIds(receiverIds == null ? List.of() : receiverIds.stream().filter(Objects::nonNull).toList());
        request.setSendScope("users");
        request.setTitle(defaultText(title, "系统通知"));
        request.setContent(defaultText(content, "你收到一条系统通知"));
        request.setActionUrl(actionUrl);
        return publishAdminSystemNotification(request, senderId);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<NotificationDTO> listAdminNotifications(Long receiverId, Long senderId, String category, String type,
                                                        Boolean readStatus, String businessStatus, Instant startTime,
                                                        Instant endTime, int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), normalizeSize(size), Sort.by(Sort.Direction.DESC, "createdAt"));
        return notificationRepository.pageAdmin(receiverId, senderId, blankToNull(category), blankToNull(type),
                readStatus, blankToNull(businessStatus), startTime, endTime, pageable).map(this::toDTO);
    }

    @Override
    @Transactional(readOnly = true)
    public NotificationDTO getAdminNotification(Long id) {
        if (id == null) {
            return null;
        }
        return notificationRepository.findById(id)
                .filter(notification -> notification.getDeletedAt() == null)
                .map(this::toDTO)
                .orElse(null);
    }

    @Override
    @Transactional
    public boolean adminDeleteNotification(Long id) {
        return id != null && notificationRepository.adminSoftDeleteById(id) > 0;
    }

    @Override
    @Transactional
    public int adminBatchDeleteNotifications(List<Long> ids) {
        List<Long> safeIds = normalizeIds(ids);
        return safeIds.isEmpty() ? 0 : notificationRepository.adminSoftDeleteByIds(safeIds);
    }

    @Override
    @Transactional
    public NotificationDTO adminMarkAsRead(Long id) {
        if (id == null || notificationRepository.adminMarkAsRead(id) <= 0) {
            return null;
        }
        return getAdminNotification(id);
    }

    @Override
    @Transactional
    public int adminBatchMarkAsRead(List<Long> ids) {
        List<Long> safeIds = normalizeIds(ids);
        return safeIds.isEmpty() ? 0 : notificationRepository.adminMarkAsReadByIds(safeIds);
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, Long> adminStats() {
        Instant todayStart = LocalDate.now(ZoneId.systemDefault()).atStartOfDay(ZoneId.systemDefault()).toInstant();
        Map<String, Long> stats = new LinkedHashMap<>();
        stats.put("total", notificationRepository.countByDeletedAtIsNull());
        stats.put("unread", notificationRepository.countByReadStatusFalseAndDeletedAtIsNull());
        stats.put("today", notificationRepository.countByCreatedAtGreaterThanEqualAndDeletedAtIsNull(todayStart));
        stats.put("system", notificationRepository.countByCategoryAndDeletedAtIsNull("system"));
        stats.put("inviteRequest", notificationRepository.countByCategoryInAndDeletedAtIsNull(List.of("invite", "request")));
        return stats;
    }

    @Override
    @Transactional
    public int publishAdminSystemNotification(NotificationSystemRequest request, Long senderId) {
        if (request == null || !StringUtils.hasText(request.getTitle()) || !StringUtils.hasText(request.getContent())) {
            throw new IllegalArgumentException("标题和内容不能为空");
        }
        List<Long> receiverIds = resolveAdminReceivers(request);
        if (receiverIds.isEmpty()) {
            throw new IllegalArgumentException("请选择接收人");
        }
        String groupKey = "admin-system:" + UUID.randomUUID();
        int count = 0;
        for (Long receiverId : receiverIds) {
            Notification created = createNotification(NotificationCreateCommand.builder()
                    .receiverId(receiverId)
                    .senderId(senderId)
                    .category(defaultText(request.getCategory(), "system"))
                    .type(defaultText(request.getType(), "system"))
                    .title(request.getTitle().trim())
                    .content(request.getContent().trim())
                    .targetType("system")
                    .eventKey(groupKey + ":receiver:" + receiverId)
                    .actionUrl(blankToNull(request.getActionUrl()))
                    .businessStatus("open")
                    .priority(request.getPriority() == null ? 0 : request.getPriority())
                    .payload(request.getPayload())
                    .expiresAt(request.getExpiresAt())
                    .build());
            if (created != null) {
                count++;
            }
        }
        return count;
    }

    private Notification createNotificationForReceiver(NotificationCreateCommand command, Long receiverId, boolean multipleReceivers) {
        if (receiverId == null) {
            return null;
        }
        if (command.getSenderId() != null && Objects.equals(receiverId, command.getSenderId())) {
            writeEventLog(command, safeEventKey(command, receiverId), 0, "skipped", "sender equals receiver");
            return null;
        }

        String eventKey = safeEventKey(command, multipleReceivers ? receiverId : null);
        if (notificationRepository.existsByEventKey(eventKey)) {
            writeEventLog(command, eventKey, 0, "skipped", "duplicate event_key");
            return null;
        }

        Instant now = Instant.now();
        String payloadJson = resolvePayloadJson(command);
        Notification notification = new Notification();
        notification.setReceiverId(receiverId);
        notification.setSenderId(command.getSenderId());
        notification.setCategory(defaultText(command.getCategory(), "system"));
        notification.setType(defaultText(command.getType(), "system"));
        notification.setTitle(defaultText(command.getTitle(), defaultTitle(command.getType())));
        notification.setContent(defaultText(command.getContent(), ""));
        notification.setReadStatus(Boolean.FALSE);
        notification.setTargetType(blankToNull(command.getTargetType()));
        notification.setTargetId(command.getTargetId());
        notification.setSourceType(blankToNull(command.getSourceType()));
        notification.setSourceId(command.getSourceId());
        notification.setEventKey(eventKey);
        notification.setActionUrl(resolveActionUrl(command, payloadJson));
        notification.setBusinessStatus(defaultText(command.getBusinessStatus(), "open"));
        notification.setPriority(command.getPriority() == null ? 0 : command.getPriority());
        notification.setPayloadJson(payloadJson);
        notification.setCreatedAt(now);
        notification.setUpdatedAt(now);
        notification.setExpiresAt(command.getExpiresAt());

        try {
            Notification saved = notificationRepository.save(notification);
            saveDelivery(saved, now);
            writeEventLog(command, eventKey, 1, "dispatched", null);
            return saved;
        } catch (DataIntegrityViolationException ex) {
            writeEventLog(command, eventKey, 0, "skipped", "duplicate event_key");
            return null;
        } catch (RuntimeException ex) {
            writeEventLog(command, eventKey, 0, "failed", ex.getMessage());
            throw ex;
        }
    }

    private void saveDelivery(Notification notification, Instant now) {
        NotificationDelivery delivery = new NotificationDelivery();
        delivery.setNotificationId(notification.getId());
        delivery.setChannel("in_app");
        delivery.setStatus("sent");
        delivery.setRetryCount(0);
        delivery.setSentAt(now);
        delivery.setCreatedAt(now);
        delivery.setUpdatedAt(now);
        notificationDeliveryRepository.save(delivery);
    }

    private void writeEventLog(NotificationCreateCommand command, String eventKey, int receiverCount, String status, String errorMessage) {
        if (!StringUtils.hasText(eventKey)) {
            return;
        }
        Instant now = Instant.now();
        NotificationEventLog log = notificationEventLogRepository.findByEventKey(eventKey).orElseGet(NotificationEventLog::new);
        if (log.getCreatedAt() == null) {
            log.setCreatedAt(now);
        }
        log.setEventKey(eventKey);
        log.setCategory(defaultText(command.getCategory(), "system"));
        log.setType(defaultText(command.getType(), "system"));
        log.setSenderId(command.getSenderId());
        log.setSourceType(blankToNull(command.getSourceType()));
        log.setSourceId(command.getSourceId());
        log.setTargetType(blankToNull(command.getTargetType()));
        log.setTargetId(command.getTargetId());
        log.setReceiverCount(receiverCount);
        log.setStatus(status);
        log.setErrorMessage(errorMessage == null ? null : truncate(errorMessage, 500));
        log.setPayloadJson(resolvePayloadJson(command));
        log.setUpdatedAt(now);
        notificationEventLogRepository.save(log);
    }

    private NotificationDTO toDTO(Notification notification) {
        UserInfo sender = notification.getSenderId() == null ? null : userInfoRepository.findById(notification.getSenderId()).orElse(null);
        return NotificationDTO.builder()
                .id(notification.getId())
                .receiverId(notification.getReceiverId())
                .senderId(notification.getSenderId())
                .senderName(resolveDisplayName(sender))
                .senderAvatar(sender == null ? null : sender.getAvatarUrl())
                .category(notification.getCategory())
                .type(notification.getType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .readStatus(Boolean.TRUE.equals(notification.getReadStatus()))
                .readAt(notification.getReadAt())
                .targetType(notification.getTargetType())
                .targetId(notification.getTargetId())
                .sourceType(notification.getSourceType())
                .sourceId(notification.getSourceId())
                .actionUrl(notification.getActionUrl())
                .businessStatus(notification.getBusinessStatus())
                .priority(notification.getPriority())
                .payloadJson(notification.getPayloadJson())
                .createdAt(notification.getCreatedAt())
                .targetTitle(resolveTargetTitle(notification))
                .preview(buildPreview(notification.getContent()))
                .build();
    }

    private String resolveTargetTitle(Notification notification) {
        String targetType = notification.getTargetType();
        Long targetId = notification.getTargetId();
        if (!StringUtils.hasText(targetType) || targetId == null) {
            return null;
        }
        if ("blog".equalsIgnoreCase(targetType)) {
            return blogRepository.findById(targetId).map(Blog::getTitle).orElse(null);
        }
        if ("project".equalsIgnoreCase(targetType) || "invitation".equalsIgnoreCase(targetType) || "join_request".equalsIgnoreCase(targetType)) {
            Long projectId = extractLongFromPayload(notification.getPayloadJson(), "projectId").orElse(targetId);
            return projectRepository.findById(projectId).map(Project::getName).orElse(null);
        }
        return null;
    }

    private Optional<Long> extractLongFromPayload(String payloadJson, String fieldName) {
        if (!StringUtils.hasText(payloadJson) || !StringUtils.hasText(fieldName)) {
            return Optional.empty();
        }
        try {
            JsonNode node = objectMapper.readTree(payloadJson).path(fieldName);
            return node.canConvertToLong() ? Optional.of(node.asLong()) : Optional.empty();
        } catch (Exception ignored) {
            return Optional.empty();
        }
    }

    private String resolveActionUrl(NotificationCreateCommand command, String payloadJson) {
        String actionUrl = normalizeActionUrl(command.getActionUrl());
        if (actionUrl != null) {
            return actionUrl;
        }

        String type = blankToNull(command.getType());
        Long projectId = firstLong(
                extractLongFromPayload(payloadJson, "projectId"),
                extractLongFromPayload(payloadJson, "project_id"),
                "project".equalsIgnoreCase(command.getTargetType()) ? Optional.ofNullable(command.getTargetId()) : Optional.empty());
        Long blogId = firstLong(
                extractLongFromPayload(payloadJson, "blogId"),
                extractLongFromPayload(payloadJson, "blog_id"),
                "blog".equalsIgnoreCase(command.getTargetType()) ? Optional.ofNullable(command.getTargetId()) : Optional.empty());
        Long commentId = firstLong(
                extractLongFromPayload(payloadJson, "commentId"),
                extractLongFromPayload(payloadJson, "comment_id"),
                "comment".equalsIgnoreCase(command.getSourceType()) ? Optional.ofNullable(command.getSourceId()) : Optional.empty());

        if (blogId != null && type != null && Set.of("comment", "reply", "like", "collect").contains(type)) {
            return commentId == null ? "/blog/" + blogId : "/blog/" + blogId + "?commentId=" + commentId + "&highlight=true";
        }
        if ("project_invitation".equals(type)) {
            StringBuilder url = new StringBuilder("/myproject?tab=invitations");
            if (projectId != null) {
                url.append("&projectId=").append(projectId);
            }
            if (command.getSourceId() != null) {
                url.append("&invitationId=").append(command.getSourceId());
            }
            return url.toString();
        }
        if ("project_join_request".equals(type) && projectId != null) {
            StringBuilder url = new StringBuilder("/projectmanage?projectId=").append(projectId).append("&tab=member-manage");
            if (command.getSourceId() != null) {
                url.append("&requestId=").append(command.getSourceId());
            }
            return url.toString();
        }
        if (projectId != null && type != null && Set.of("project_invitation_accepted", "project_invitation_rejected", "project_join_approved", "project_join_rejected", "project_star").contains(type)) {
            return "/projectdetail?projectId=" + projectId;
        }
        return null;
    }

    private String normalizeActionUrl(String actionUrl) {
        String normalized = blankToNull(actionUrl);
        if (normalized == null) {
            return null;
        }
        if (normalized.startsWith("/projectdetail?id=")) {
            return normalized.replaceFirst("^/projectdetail\\?id=", "/projectdetail?projectId=");
        }
        if (normalized.startsWith("/projectmanage?id=")) {
            return normalized
                    .replaceFirst("^/projectmanage\\?id=", "/projectmanage?projectId=")
                    .replace("tab=joinRequests", "tab=member-manage");
        }
        return normalized.replace("tab=joinRequests", "tab=member-manage");
    }

    @SafeVarargs
    private final Long firstLong(Optional<Long>... values) {
        if (values == null) {
            return null;
        }
        for (Optional<Long> value : values) {
            if (value != null && value.isPresent()) {
                return value.get();
            }
        }
        return null;
    }

    private List<Long> resolveReceiverIds(NotificationCreateCommand command) {
        List<Long> ids = new ArrayList<>();
        if (command.getReceiverId() != null) {
            ids.add(command.getReceiverId());
        }
        if (command.getReceiverIds() != null) {
            ids.addAll(command.getReceiverIds());
        }
        return normalizeIds(ids);
    }

    private List<Long> resolveAdminReceivers(NotificationSystemRequest request) {
        String scope = defaultText(request.getSendScope(), "users");
        if ("all".equalsIgnoreCase(scope)) {
            return userInfoRepository.findAll().stream().map(UserInfo::getId).filter(Objects::nonNull).distinct().toList();
        }
        if ("roles".equalsIgnoreCase(scope)) {
            List<Integer> roleIds = request.getRoleIds() == null ? List.of() : request.getRoleIds().stream().filter(Objects::nonNull).distinct().toList();
            return roleIds.isEmpty() ? List.of() : userInfoRepository.findByRoleIdIn(roleIds).stream().map(UserInfo::getId).filter(Objects::nonNull).distinct().toList();
        }
        List<Long> ids = new ArrayList<>();
        if (request.getReceiverId() != null) {
            ids.add(request.getReceiverId());
        }
        if (request.getReceiverIds() != null) {
            ids.addAll(request.getReceiverIds());
        }
        return normalizeIds(ids);
    }

    private String safeEventKey(NotificationCreateCommand command, Long receiverIdForMulti) {
        String base = blankToNull(command.getEventKey());
        if (base == null) {
            base = String.join(":",
                    defaultText(command.getCategory(), "system"),
                    defaultText(command.getType(), "system"),
                    defaultText(command.getSourceType(), "manual"),
                    String.valueOf(command.getSourceId() == null ? UUID.randomUUID() : command.getSourceId()),
                    String.valueOf(receiverIdForMulti == null ? command.getReceiverId() : receiverIdForMulti));
        } else if (receiverIdForMulti != null && !base.contains(":receiver:")) {
            base = base + ":receiver:" + receiverIdForMulti;
        }
        return truncate(base, 191);
    }

    private String resolvePayloadJson(NotificationCreateCommand command) {
        if (StringUtils.hasText(command.getPayloadJson())) {
            return command.getPayloadJson();
        }
        if (command.getPayload() == null || command.getPayload().isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(command.getPayload());
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    private String resolveDisplayName(UserInfo sender) {
        if (sender == null) {
            return "系统";
        }
        if (StringUtils.hasText(sender.getNickname())) {
            return sender.getNickname();
        }
        if (StringUtils.hasText(sender.getUsername())) {
            return sender.getUsername();
        }
        return "用户" + sender.getId();
    }

    private String defaultTitle(String type) {
        if (!StringUtils.hasText(type)) {
            return "消息通知";
        }
        return switch (type) {
            case "comment" -> "新的评论";
            case "reply" -> "新的回复";
            case "like" -> "新的点赞";
            case "collect" -> "新的收藏";
            case "project_invitation" -> "项目邀请";
            case "project_invitation_accepted" -> "邀请已接受";
            case "project_invitation_rejected" -> "邀请已拒绝";
            case "project_join_request" -> "加入申请";
            case "project_join_approved" -> "申请已通过";
            case "project_join_rejected" -> "申请已拒绝";
            case "project_star" -> "项目被收藏";
            case "system" -> "系统通知";
            default -> "消息通知";
        };
    }

    private String buildPreview(String content) {
        if (!StringUtils.hasText(content)) {
            return "";
        }
        String normalized = content.trim().replaceAll("\\s+", " ");
        return normalized.length() > 80 ? normalized.substring(0, 80) + "..." : normalized;
    }

    private String defaultText(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private String truncate(String value, int max) {
        return value == null || value.length() <= max ? value : value.substring(0, max);
    }

    private int normalizeSize(int size) {
        return Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
    }

    private List<Long> normalizeIds(Collection<Long> ids) {
        if (ids == null) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
    }
}
