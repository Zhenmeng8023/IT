package com.alikeyou.itmoduleinteractive.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageDTO {

    private Long id;

    private Long conversationId;

    private Long senderId;

    private String content;

    private String messageType;

    private Instant sentAt;

    private Boolean isRead;

    private String senderName;

    private String senderAvatar;
}
