package com.alikeyou.itmodulecommon.notification;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationCreateCommand {

    private Long receiverId;

    @Builder.Default
    private List<Long> receiverIds = List.of();

    private Long senderId;

    @Builder.Default
    private String category = "system";

    @Builder.Default
    private String type = "system";

    private String title;

    private String content;

    private String targetType;

    private Long targetId;

    private String sourceType;

    private Long sourceId;

    private String eventKey;

    private String actionUrl;

    @Builder.Default
    private String businessStatus = "open";

    @Builder.Default
    private Integer priority = 0;

    private Map<String, Object> payload;

    private String payloadJson;

    private Instant expiresAt;
}
