package com.alikeyou.itmodulecircle.dto;

import com.alikeyou.itmodulecircle.entity.Message;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {

        private Long id;

        private Long conversationId;

        private Long senderId;

        private String senderName;

        private String senderAvatar;

        private String content;

        private String messageType;

        private Instant sentAt;

        private Boolean isRead;
}
