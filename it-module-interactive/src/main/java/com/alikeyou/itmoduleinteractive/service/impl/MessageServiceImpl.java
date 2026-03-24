package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmoduleinteractive.dto.MessageDTO;
import com.alikeyou.itmoduleinteractive.entity.Message;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.repository.MessageRepository;
import com.alikeyou.itmoduleinteractive.repository.NotificationRepository;
import com.alikeyou.itmoduleinteractive.service.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Override
    @Transactional
    public MessageDTO sendMessage(MessageDTO dto, Long userId) {
        Message message = new Message();
        message.setConversationId(dto.getConversationId());
        message.setSenderId(userId);
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        message.setSentAt(Instant.now());
        message.setIsRead(false);

        Message saved = messageRepository.save(message);

        return convertToDTO(saved);
    }

    @Override
    public List<MessageDTO> getConversationMessages(Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getRecentMessages(Long conversationId, int limit) {
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId);
        return messages.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        List<Message> unreadMessages = messageRepository.findByConversationIdAndIsReadFalse(conversationId);
        unreadMessages.forEach(message -> message.setIsRead(true));
        messageRepository.saveAll(unreadMessages);
    }

    @Override
    public long getUnreadCount(Long conversationId) {
        return messageRepository.countByConversationIdAndIsReadFalse(conversationId);
    }

    private MessageDTO convertToDTO(Message message) {
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setSentAt(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        return dto;
    }
}
