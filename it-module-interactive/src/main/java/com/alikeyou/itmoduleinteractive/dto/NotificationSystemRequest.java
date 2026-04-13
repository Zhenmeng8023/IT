package com.alikeyou.itmoduleinteractive.dto;

import lombok.Data;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@Data
public class NotificationSystemRequest {

    private Long receiverId;
    private List<Long> receiverIds;
    private List<Integer> roleIds;
    private String sendScope;
    private String title;
    private String content;
    private String actionUrl;
    private String category;
    private String type;
    private Integer priority;
    private Instant expiresAt;
    private Map<String, Object> payload;
}
