package com.alikeyou.itmodulecommon.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NotificationPublisher {

    private final ApplicationEventPublisher applicationEventPublisher;

    public void publish(NotificationCreateCommand command) {
        if (command == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new NotificationPublishEvent(command));
    }

    public void updateBusinessStatus(String sourceType, Long sourceId, String businessStatus) {
        if (sourceType == null || sourceId == null || businessStatus == null) {
            return;
        }
        applicationEventPublisher.publishEvent(new NotificationBusinessStatusUpdateEvent(sourceType, sourceId, businessStatus));
    }
}
