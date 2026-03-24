package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.repository.NotificationRepository;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class NotificationServiceImpl implements NotificationService {

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    public List<Notification> getUserNotifications(Long userId) {
        return notificationRepository.findByReceiverIdOrderByCreatedAtDesc(userId);
    }

    @Override
    public List<Notification> getUserUnreadNotifications(Long userId) {
        return notificationRepository.findByReceiverIdAndReadStatusFalseOrderByCreatedAtDesc(userId);
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
                    return notificationRepository.save(notification);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteNotification(Long notificationId, Long userId) {
        notificationRepository.findById(notificationId)
                .filter(notification -> notification.getReceiverId().equals(userId))
                .ifPresent(notificationRepository::delete);
    }

    @Override
    @Transactional
    public void clearUserNotifications(Long userId) {
        notificationRepository.deleteByReceiverId(userId);
    }
}
