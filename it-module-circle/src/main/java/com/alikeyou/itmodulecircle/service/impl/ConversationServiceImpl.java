package com.alikeyou.itmodulecircle.service.impl;

import com.alikeyou.itmodulecircle.dto.ConversationResponse;
import com.alikeyou.itmodulecircle.dto.MessageRequest;
import com.alikeyou.itmodulecircle.dto.MessageResponse;
import com.alikeyou.itmodulecircle.entity.Conversation;
import com.alikeyou.itmodulecircle.entity.Message;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecircle.exception.CircleException;
import com.alikeyou.itmodulecircle.repository.ConversationRepository;
import com.alikeyou.itmodulecircle.repository.MessageRepository;
import com.alikeyou.itmodulecircle.service.ConversationService;
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

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private com.alikeyou.itmodulelogin.repository.UserRepository userRepository;

    @Override
    public List<ConversationResponse> getAllConversations() {
        List<Conversation> conversations = conversationRepository.findAll();
        return conversations.stream()
                .map(this::convertToConversationResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public Conversation createConversation(Conversation conversation) {
        if (conversation.getType() == null || conversation.getType().isEmpty()) {
            throw new CircleException("会话类型不能为空");
        }

        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());

        return conversationRepository.save(conversation);
    }

    @Override
    public Optional<Conversation> getConversationById(Long id) {
        return conversationRepository.findById(id);
    }

    @Override
    @Transactional
    public void deleteConversation(Long id) {
        Conversation conversation = getConversationById(id)
                .orElseThrow(() -> new CircleException("会话不存在"));

        messageRepository.deleteByConversationId(id);
        conversationRepository.delete(conversation);
    }

    @Override
    @Transactional
    public Message sendMessage(Long conversationId, Long senderId, MessageRequest request) {
        Conversation conversation = getConversationById(conversationId)
                .orElseThrow(() -> new CircleException("会话不存在"));

        UserInfo sender = userRepository.findById(senderId)
                .orElseThrow(() -> new CircleException("发送者不存在，ID: " + senderId));

        Message message = new Message();
        message.setConversation(conversation);
        message.setSenderId(senderId);
        message.setContent(request.getContent());
        message.setMessageType(request.getMessageType() != null ? request.getMessageType() : "text");
        message.setSentAt(Instant.now());
        message.setIsRead(false);

        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        return messageRepository.save(message);
    }


    @Override
    public List<MessageResponse> getConversationMessages(Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageResponse> getConversationMessagesAfter(Long conversationId, Instant since) {
        List<Message> messages = messageRepository.findByConversationIdAndSentAtAfterOrderBySentAtAsc(conversationId, since);
        return messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessageAsRead(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new CircleException("消息不存在"));
        message.setIsRead(true);
        messageRepository.save(message);
    }

    @Override
    @Transactional
    public void markAllMessagesAsRead(Long conversationId) {
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        for (Message message : messages) {
            message.setIsRead(true);
        }
        messageRepository.saveAll(messages);
    }

    @Override
    public Long getUnreadMessageCount(Long conversationId) {
        return messageRepository.countByConversationIdAndIsReadFalse(conversationId);
    }

    @Override
    public ConversationResponse getConversationDetail(Long conversationId, Long userId) {
        Conversation conversation = getConversationById(conversationId)
                .orElseThrow(() -> new CircleException("会话不存在"));

        List<Message> latestMessages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        if (latestMessages.size() > 20) {
            latestMessages = latestMessages.subList(latestMessages.size() - 20, latestMessages.size());
        }

        Long unreadCount = getUnreadMessageCount(conversationId);

        return convertToConversationResponseWithMessages(conversation, latestMessages, unreadCount);
    }

    private ConversationResponse convertToConversationResponse(Conversation conversation) {
        String creatorName = null;
        if (conversation.getCreatorId() != null) {
            UserInfo creator = userRepository.findById(conversation.getCreatorId()).orElse(null);
            if (creator != null && creator.getNickname() != null) {
                creatorName = creator.getNickname();
            }
        }

        return ConversationResponse.builder()
                .id(conversation.getId())
                .type(conversation.getType())
                .name(conversation.getName())
                .creatorId(conversation.getCreatorId())
                .creatorName(creatorName)
                .createdAt(conversation.getCreatedAt())
                .updatedAt(conversation.getUpdatedAt())
                .build();
    }
    private MessageResponse convertToMessageResponse(Message message) {
        UserInfo sender = userRepository.findById(message.getSenderId()).orElse(null);

        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversation().getId())
                .senderId(message.getSenderId())
                .senderName(sender != null && sender.getNickname() != null ? sender.getNickname() : "未知用户")
                .senderAvatar(sender != null ? sender.getAvatarUrl() : null)
                .content(message.getContent())
                .messageType(message.getMessageType())
                .sentAt(message.getSentAt())
                .isRead(message.getIsRead())
                .build();
    }

    private ConversationResponse convertToConversationResponseWithMessages(Conversation conversation, List<Message> messages, Long unreadCount) {
        ConversationResponse response = convertToConversationResponse(conversation);
        response.setUnreadCount(Math.toIntExact(unreadCount));
        response.setLatestMessages(messages.stream()
                .map(this::convertToMessageResponse)
                .collect(Collectors.toList()));
        return response;
    }
}
