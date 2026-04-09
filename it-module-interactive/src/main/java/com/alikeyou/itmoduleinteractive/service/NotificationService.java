package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.entity.Notification;

import java.util.List;

public interface NotificationService {

    Notification createNotification(Notification notification);

    List<Notification> getUserNotifications(Long userId);

    List<Notification> getUserUnreadNotifications(Long userId);

    long countUserUnreadNotifications(Long userId);

    Notification markAsRead(Long notificationId, Long userId);

    long markAllAsRead(Long userId);

    void deleteNotification(Long notificationId, Long userId);

    void clearUserNotifications(Long userId);
}
