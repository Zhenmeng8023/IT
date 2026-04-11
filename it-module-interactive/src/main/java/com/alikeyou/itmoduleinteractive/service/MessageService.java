package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.dto.MessageDTO;

import java.util.List;

public interface MessageService {

    MessageDTO sendMessage(MessageDTO dto, Long userId);

    List<MessageDTO> getConversationMessages(Long conversationId);

    List<MessageDTO> getRecentMessages(Long conversationId, int limit);

    void markMessagesAsRead(Long conversationId, Long userId);

    long getUnreadCount(Long conversationId);
}
