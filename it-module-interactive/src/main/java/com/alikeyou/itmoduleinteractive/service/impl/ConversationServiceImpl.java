package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.dto.ConversationDTO;
import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.repository.ConversationRepository;
import com.alikeyou.itmoduleinteractive.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Override
    @Transactional
    public ConversationDTO createConversation(ConversationDTO dto, Long userId) {
        Conversation conversation = new Conversation();
        conversation.setType(dto.getType());
        conversation.setName(dto.getName());
        conversation.setCreatorId(userId);
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());

        Conversation saved = conversationRepository.save(conversation);
        return convertToDTO(saved);
    }

    @Override
    public Optional<ConversationDTO> getConversationById(Long id) {
        return conversationRepository.findById(id).map(this::convertToDTO);
    }

    @Override
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Conversation> conversations = conversationRepository.findAll();
        // 这里可以根据实际业务逻辑过滤用户相关的会话
        return conversations.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationDTO updateConversationLastMessage(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> {
                    conversation.setUpdatedAt(Instant.now());
                    Conversation updated = conversationRepository.save(conversation);
                    return convertToDTO(updated);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        conversationRepository.findById(conversationId)
                .ifPresent(conversation -> {
                    if (conversation.getCreatorId().equals(userId)) {
                        conversationRepository.delete(conversation);
                    }
                });
    }

    private ConversationDTO convertToDTO(Conversation conversation) {
        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setName(conversation.getName());
        dto.setCreatorId(conversation.getCreatorId());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        return dto;
    }
}
