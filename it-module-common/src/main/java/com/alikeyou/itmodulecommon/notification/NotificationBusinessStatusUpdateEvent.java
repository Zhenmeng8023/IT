package com.alikeyou.itmodulecommon.notification;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class NotificationBusinessStatusUpdateEvent {

    private final String sourceType;
    private final Long sourceId;
    private final String businessStatus;
}
