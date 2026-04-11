package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.dto.ConversationDTO;

import java.util.List;
import java.util.Optional;

public interface ConversationService {

    ConversationDTO createConversation(ConversationDTO dto, Long userId);

    Optional<ConversationDTO> getConversationById(Long id, Long userId);

    List<ConversationDTO> getUserConversations(Long userId);

    ConversationDTO updateConversationLastMessage(Long conversationId);

    void deleteConversation(Long conversationId, Long userId);
}
