package com.alikeyou.itmoduleinteractive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationDTO {

    private Long id;

    private String type;

    private String name;

    private Long creatorId;

    private List<Long> participantIds;

    private Integer participantCount;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer unreadCount;

    private String lastMessageContent;

    private Instant lastMessageTime;
}
