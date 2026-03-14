package com.alikeyou.itmodulecircle.service;

import com.alikeyou.itmodulecircle.dto.ConversationResponse;
import com.alikeyou.itmodulecircle.dto.MessageRequest;
import com.alikeyou.itmodulecircle.dto.MessageResponse;
import com.alikeyou.itmodulecircle.entity.Conversation;
import com.alikeyou.itmodulecircle.entity.Message;

import java.util.List;
import java.util.Optional;

public interface ConversationService {

    Conversation createConversation(Conversation conversation);

    Optional<Conversation> getConversationById(Long id);

    List<ConversationResponse> getAllConversations();

    void deleteConversation(Long id);

    Message sendMessage(Long conversationId, Long senderId, MessageRequest request);

    List<MessageResponse> getConversationMessages(Long conversationId);

    List<MessageResponse> getConversationMessagesAfter(Long conversationId, java.time.Instant since);

    void markMessageAsRead(Long messageId);

    void markAllMessagesAsRead(Long conversationId);

    Long getUnreadMessageCount(Long conversationId);

    ConversationResponse getConversationDetail(Long conversationId, Long userId);
}
