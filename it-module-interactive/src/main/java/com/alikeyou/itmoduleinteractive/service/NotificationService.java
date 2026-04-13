package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmodulecommon.notification.NotificationCreateCommand;
import com.alikeyou.itmoduleinteractive.dto.NotificationDTO;
import com.alikeyou.itmoduleinteractive.dto.NotificationSystemRequest;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import org.springframework.data.domain.Page;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface NotificationService {

    Notification createNotification(NotificationCreateCommand command);

    Notification createNotification(Notification notification);

    Page<NotificationDTO> listMyNotifications(Long userId, int page, int size, String category, String type, Boolean readStatus);

    long countUnread(Long userId);

    Map<String, Long> countUnreadByCategory(Long userId);

    NotificationDTO markAsRead(Long notificationId, Long userId);

    long markAllAsRead(Long userId, String category);

    boolean deleteNotification(Long notificationId, Long userId);

    long clearUserNotifications(Long userId);

    void updateBusinessStatus(String sourceType, Long sourceId, String businessStatus);

    Notification publishSystemNotification(Long receiverId, String title, String content, String actionUrl, Long senderId);

    int publishBroadcastNotification(Collection<Long> receiverIds, String title, String content, String actionUrl, Long senderId);

    Page<NotificationDTO> listAdminNotifications(Long receiverId, Long senderId, String category, String type,
                                                 Boolean readStatus, String businessStatus, Instant startTime,
                                                 Instant endTime, int page, int size);

    NotificationDTO getAdminNotification(Long id);

    boolean adminDeleteNotification(Long id);

    int adminBatchDeleteNotifications(List<Long> ids);

    NotificationDTO adminMarkAsRead(Long id);

    int adminBatchMarkAsRead(List<Long> ids);

    Map<String, Long> adminStats();

    int publishAdminSystemNotification(NotificationSystemRequest request, Long senderId);
}
