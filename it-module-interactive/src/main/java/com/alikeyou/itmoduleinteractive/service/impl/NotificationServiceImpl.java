package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.realtime.UserRealtimeEmitterRegistry;
import com.alikeyou.itmoduleinteractive.repository.NotificationRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import com.alikeyou.itmoduleinteractive.support.NotificationViewAssembler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private NotificationViewAssembler notificationViewAssembler;

    @Autowired
    private UserRealtimeEmitterRegistry emitterRegistry;

    @Override
    @Transactional
    public Notification createNotification(Notification notification) {
        if (notification.getCreatedAt() == null) {
            notification.setCreatedAt(Instant.now());
        }
        if (notification.getReadStatus() == null) {
            notification.setReadStatus(Boolean.FALSE);
        }

        Notification saved = notificationRepository.save(notification);
        Notification enriched = notificationViewAssembler.enrich(saved);
        pushState(saved.getReceiverId(), "notification-created", Map.of(
                "notification", enriched,
                "unreadCount", countUserUnreadNotifications(saved.getReceiverId())
        ));
        return enriched;
    }

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationViewAssembler.enrichAll(notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId));
    }

    @Override
    public List<Notification> getUserUnreadNotifications(Long userId) {
        return notificationViewAssembler.enrichAll(notificationRepository.findByReceiverIdAndReadStatusFalseOrderByCreatedAtDesc(userId));
    }

    @Override
    public long countUserUnreadNotifications(Long userId) {
        return notificationRepository.countByReceiverIdAndReadStatusFalse(userId);
    }

    @Override
    @Transactional
    public Notification markAsRead(Long notificationId, Long userId) {
        return notificationRepository.findById(notificationId)
                .filter(notification -> notification.getReceiverId().equals(userId))
                .map(notification -> {
                    notification.setReadStatus(true);
                    Notification saved = notificationRepository.save(notification);
                    Notification enriched = notificationViewAssembler.enrich(saved);
                    pushState(userId, "notification-read", Map.of(
                            "notificationId", notificationId,
                            "notification", enriched,
                            "unreadCount", countUserUnreadNotifications(userId)
                    ));
                    return enriched;
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public long markAllAsRead(Long userId) {
        List<Notification> unreadNotifications = notificationRepository.findByReceiverIdAndReadStatusFalseOrderByCreatedAtDesc(userId);
        unreadNotifications.forEach(notification -> notification.setReadStatus(true));
        notificationRepository.saveAll(unreadNotifications);
        pushState(userId, "notification-read-all", Map.of(
                "unreadCount", 0
        ));
        return unreadNotifications.size();
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId)
                .filter(notification -> notification.getReceiverId().equals(userId))
                .ifPresent(notification -> {
                    notificationRepository.delete(notification);
                    pushState(userId, "notification-deleted", Map.of(
                            "notificationId", notificationId,
                            "unreadCount", countUserUnreadNotifications(userId)
                    ));
                });
    }

    @Override
    @Transactional
    public void clearUserNotifications(Long userId) {
        notificationRepository.deleteByReceiverId(userId);
        pushState(userId, "notification-cleared", Map.of(
                "unreadCount", 0
        ));
    }

    private void pushState(Long userId, String eventName, Object payload) {
        emitterRegistry.pushToUser(userId, eventName, payload);
    }
}
