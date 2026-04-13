package com.alikeyou.itmodulecommon.notification;

import lombok.Getter;

@Getter
public class NotificationPublishEvent {

    private final NotificationCreateCommand command;

    public NotificationPublishEvent(NotificationCreateCommand command) {
        this.command = command;
    }
}
