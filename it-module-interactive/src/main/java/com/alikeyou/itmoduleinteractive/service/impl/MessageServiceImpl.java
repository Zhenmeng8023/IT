package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.dto.MessageDTO;
import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.entity.ConversationParticipant;
import com.alikeyou.itmoduleinteractive.entity.Message;
import com.alikeyou.itmoduleinteractive.entity.Notification;
import com.alikeyou.itmoduleinteractive.realtime.UserRealtimeEmitterRegistry;
import com.alikeyou.itmoduleinteractive.repository.ConversationParticipantRepository;
import com.alikeyou.itmoduleinteractive.repository.ConversationRepository;
import com.alikeyou.itmoduleinteractive.repository.MessageRepository;
import com.alikeyou.itmoduleinteractive.service.MessageService;
import com.alikeyou.itmoduleinteractive.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class MessageServiceImpl implements MessageService {

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private NotificationService notificationService;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Autowired
    private UserRealtimeEmitterRegistry emitterRegistry;

    @Override
    @Transactional
    public MessageDTO sendMessage(MessageDTO dto, Long userId) {
        Conversation conversation = requireConversation(dto.getConversationId());
        ConversationParticipant senderParticipant = requireParticipant(dto.getConversationId(), userId);

        Message message = new Message();
        message.setConversationId(dto.getConversationId());
        message.setSenderId(userId);
        message.setContent(dto.getContent());
        message.setMessageType(dto.getMessageType() != null ? dto.getMessageType() : "text");
        message.setSentAt(Instant.now());
        message.setIsRead(false);

        Message saved = messageRepository.save(message);
        senderParticipant.setLastReadAt(saved.getSentAt());
        conversationParticipantRepository.save(senderParticipant);
        conversation.setUpdatedAt(saved.getSentAt());
        conversationRepository.save(conversation);

        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversationIdOrderByJoinedAtAsc(dto.getConversationId());
        MessageDTO payload = convertToDTO(saved);
        List<Long> participantIds = participants.stream()
                .map(ConversationParticipant::getUserId)
                .toList();

        emitterRegistry.pushToUsers(participantIds, "message-created", payload);
        participantIds.forEach(participantId -> emitterRegistry.pushToUser(participantId, "conversation-updated",
                buildConversationEvent(dto.getConversationId(), participantId, saved)));
        createMessageNotifications(dto.getConversationId(), userId, participants, saved);

        return payload;
    }

    @Override
    public List<MessageDTO> getConversationMessages(Long conversationId, Long userId) {
        requireParticipant(conversationId, userId);
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
        return messages.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public List<MessageDTO> getRecentMessages(Long conversationId, Long userId, int limit) {
        requireParticipant(conversationId, userId);
        List<Message> messages = messageRepository.findByConversationIdOrderBySentAtDesc(conversationId);
        return messages.stream()
                .limit(limit)
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void markMessagesAsRead(Long conversationId, Long userId) {
        ConversationParticipant participant = requireParticipant(conversationId, userId);
        Instant now = Instant.now();
        participant.setLastReadAt(now);
        conversationParticipantRepository.save(participant);

        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversationIdOrderByJoinedAtAsc(conversationId);
        if (participants.size() <= 2) {
            List<Message> unreadMessages = messageRepository.findByConversationIdAndIsReadFalse(conversationId).stream()
                    .filter(message -> !Objects.equals(message.getSenderId(), userId))
                    .toList();
            unreadMessages.forEach(message -> message.setIsRead(true));
            if (!unreadMessages.isEmpty()) {
                messageRepository.saveAll(unreadMessages);
            }
        }

        emitterRegistry.pushToUser(userId, "conversation-read", buildConversationReadEvent(conversationId, userId));
    }

    @Override
    public long getUnreadCount(Long conversationId, Long userId) {
        ConversationParticipant participant = requireParticipant(conversationId, userId);
        return messageRepository.countUnreadForUser(conversationId, userId, participant.getLastReadAt());
    }

    private MessageDTO convertToDTO(Message message) {
        UserInfo sender = userInfoRepository.findById(message.getSenderId()).orElse(null);
        MessageDTO dto = new MessageDTO();
        dto.setId(message.getId());
        dto.setConversationId(message.getConversationId());
        dto.setSenderId(message.getSenderId());
        dto.setContent(message.getContent());
        dto.setMessageType(message.getMessageType());
        dto.setSentAt(message.getSentAt());
        dto.setIsRead(message.getIsRead());
        dto.setSenderName(resolveUserName(sender));
        dto.setSenderAvatar(sender == null ? null : sender.getAvatarUrl());
        return dto;
    }

    private Conversation requireConversation(Long conversationId) {
        if (conversationId == null) {
            throw new IllegalArgumentException("会话 ID 不能为空");
        }
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new IllegalArgumentException("会话不存在"));
    }

    private ConversationParticipant requireParticipant(Long conversationId, Long userId) {
        return conversationParticipantRepository.findByConversationIdAndUserId(conversationId, userId)
                .orElseThrow(() -> new IllegalArgumentException("当前用户不在该会话中"));
    }

    private void createMessageNotifications(Long conversationId,
                                            Long senderId,
                                            List<ConversationParticipant> participants,
                                            Message message) {
        if (participants == null || participants.isEmpty() || message == null) {
            return;
        }

        String preview = buildPreview(message.getContent());
        participants.stream()
                .map(ConversationParticipant::getUserId)
                .filter(receiverId -> !Objects.equals(receiverId, senderId))
                .forEach(receiverId -> {
                    Notification notification = new Notification();
                    notification.setReceiverId(receiverId);
                    notification.setSenderId(senderId);
                    notification.setType("message");
                    notification.setContent(preview);
                    notification.setReadStatus(Boolean.FALSE);
                    notification.setTargetType("conversation");
                    notification.setTargetId(conversationId);
                    notification.setCreatedAt(message.getSentAt());
                    notificationService.createNotification(notification);
                });
    }

    private String buildPreview(String content) {
        String normalized = content == null ? "" : content.trim().replaceAll("\\s+", " ");
        if (normalized.length() <= 60) {
            return normalized;
        }
        return normalized.substring(0, 60) + "...";
    }

    private Map<String, Object> buildConversationEvent(Long conversationId, Long userId, Message message) {
        return Map.of(
                "conversationId", conversationId,
                "lastMessageContent", message.getContent(),
                "lastMessageTime", message.getSentAt(),
                "unreadCount", getUnreadCount(conversationId, userId)
        );
    }

    private Map<String, Object> buildConversationReadEvent(Long conversationId, Long userId) {
        return Map.of(
                "conversationId", conversationId,
                "userId", userId,
                "unreadCount", getUnreadCount(conversationId, userId)
        );
    }

    private String resolveUserName(UserInfo userInfo) {
        if (userInfo == null) {
            return "未知用户";
        }
        if (userInfo.getNickname() != null && !userInfo.getNickname().isBlank()) {
            return userInfo.getNickname();
        }
        if (userInfo.getUsername() != null && !userInfo.getUsername().isBlank()) {
            return userInfo.getUsername();
        }
        return "未知用户";
    }
}
