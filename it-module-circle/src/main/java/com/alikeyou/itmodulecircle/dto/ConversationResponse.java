package com.alikeyou.itmodulecircle.dto;

import com.alikeyou.itmodulecircle.entity.Conversation;
import com.alikeyou.itmodulecircle.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {

    private Long id;

    private String type;

    private String name;

    private Long creatorId;

    private String creatorName;

    private Instant createdAt;

    private Instant updatedAt;

    private Integer unreadCount;

    private List<MessageResponse> latestMessages;

}
