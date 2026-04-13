package com.alikeyou.itmoduleinteractive.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class NotificationDTO {

    private Long id;
    private Long receiverId;
    private Long senderId;
    private String senderName;
    private String senderAvatar;
    private String category;
    private String type;
    private String title;
    private String content;
    private Boolean readStatus;
    private Instant readAt;
    private String targetType;
    private Long targetId;
    private String sourceType;
    private Long sourceId;
    private String actionUrl;
    private String businessStatus;
    private Integer priority;
    private String payloadJson;
    private Instant createdAt;
    private String targetTitle;
    private String preview;
}
