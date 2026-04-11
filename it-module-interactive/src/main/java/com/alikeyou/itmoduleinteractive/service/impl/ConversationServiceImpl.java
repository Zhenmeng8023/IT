package com.alikeyou.itmoduleinteractive.service.impl;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmoduleinteractive.dto.ConversationDTO;
import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.entity.ConversationParticipant;
import com.alikeyou.itmoduleinteractive.entity.Message;
import com.alikeyou.itmoduleinteractive.repository.ConversationParticipantRepository;
import com.alikeyou.itmoduleinteractive.repository.ConversationRepository;
import com.alikeyou.itmoduleinteractive.repository.MessageRepository;
import com.alikeyou.itmoduleinteractive.service.ConversationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ConversationServiceImpl implements ConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private ConversationParticipantRepository conversationParticipantRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

    @Override
    @Transactional
    public ConversationDTO createConversation(ConversationDTO dto, Long userId) {
        Set<Long> participantIds = buildParticipantIds(dto, userId);
        String type = normalizeType(dto == null ? null : dto.getType());

        if ("private".equals(type)) {
            Optional<ConversationDTO> existing = findExistingPrivateConversation(userId, participantIds);
            if (existing.isPresent()) {
                return existing.get();
            }
        }

        Instant now = Instant.now();
        Conversation conversation = new Conversation();
        conversation.setType(type);
        conversation.setName(dto == null ? null : dto.getName());
        conversation.setCreatorId(userId);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        Conversation saved = conversationRepository.save(conversation);
        participantIds.forEach(participantId -> {
            ConversationParticipant participant = new ConversationParticipant();
            participant.setConversationId(saved.getId());
            participant.setUserId(participantId);
            participant.setJoinedAt(now);
            if (Objects.equals(participantId, userId)) {
                participant.setLastReadAt(now);
            }
            conversationParticipantRepository.save(participant);
        });
        return convertToDTO(saved, userId);
    }

    @Override
    public Optional<ConversationDTO> getConversationById(Long id, Long userId) {
        if (id == null || userId == null || !conversationParticipantRepository.existsByConversationIdAndUserId(id, userId)) {
            return Optional.empty();
        }
        return conversationRepository.findById(id).map(conversation -> convertToDTO(conversation, userId));
    }

    @Override
    public List<ConversationDTO> getUserConversations(Long userId) {
        List<Long> conversationIds = conversationParticipantRepository.findByUserIdOrderByJoinedAtDesc(userId).stream()
                .map(ConversationParticipant::getConversationId)
                .distinct()
                .toList();
        if (conversationIds.isEmpty()) {
            return List.of();
        }

        List<Conversation> conversations = conversationRepository.findByIdInOrderByUpdatedAtDesc(conversationIds);
        return conversations.stream()
                .map(conversation -> convertToDTO(conversation, userId))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ConversationDTO updateConversationLastMessage(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .map(conversation -> {
                    conversation.setUpdatedAt(Instant.now());
                    Conversation updated = conversationRepository.save(conversation);
                    return convertToDTO(updated, null);
                })
                .orElse(null);
    }

    @Override
    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        conversationRepository.findById(conversationId)
                .ifPresent(conversation -> {
                    if (Objects.equals(conversation.getCreatorId(), userId)) {
                        conversationParticipantRepository.deleteByConversationId(conversationId);
                        conversationRepository.delete(conversation);
                    }
                });
    }

    private ConversationDTO convertToDTO(Conversation conversation, Long currentUserId) {
        List<ConversationParticipant> participants = conversationParticipantRepository.findByConversationIdOrderByJoinedAtAsc(conversation.getId());
        List<Long> participantIds = participants.stream()
                .map(ConversationParticipant::getUserId)
                .toList();
        Message lastMessage = messageRepository.findFirstByConversationIdOrderBySentAtDescIdDesc(conversation.getId());

        ConversationDTO dto = new ConversationDTO();
        dto.setId(conversation.getId());
        dto.setType(conversation.getType());
        dto.setName(resolveConversationName(conversation, participantIds, currentUserId));
        dto.setCreatorId(conversation.getCreatorId());
        dto.setParticipantIds(participantIds);
        dto.setParticipantCount(participantIds.size());
        dto.setCreatedAt(conversation.getCreatedAt());
        dto.setUpdatedAt(conversation.getUpdatedAt());
        dto.setLastMessageContent(lastMessage == null ? null : lastMessage.getContent());
        dto.setLastMessageTime(lastMessage == null ? null : lastMessage.getSentAt());
        dto.setUnreadCount(resolveUnreadCount(conversation.getId(), participants, currentUserId));
        return dto;
    }

    private Optional<ConversationDTO> findExistingPrivateConversation(Long userId, Set<Long> participantIds) {
        return getUserConversations(userId).stream()
                .filter(item -> "private".equalsIgnoreCase(item.getType()))
                .filter(item -> item.getParticipantIds() != null
                        && item.getParticipantIds().size() == participantIds.size()
                        && participantIds.containsAll(item.getParticipantIds()))
                .findFirst();
    }

    private Set<Long> buildParticipantIds(ConversationDTO dto, Long userId) {
        Set<Long> ids = new LinkedHashSet<>();
        ids.add(userId);
        if (dto != null && dto.getParticipantIds() != null) {
            ids.addAll(dto.getParticipantIds().stream()
                    .filter(Objects::nonNull)
                    .collect(Collectors.toCollection(LinkedHashSet::new)));
        }
        return ids;
    }

    private String normalizeType(String type) {
        if (type == null || type.isBlank()) {
            return "private";
        }
        String normalized = type.trim().toLowerCase();
        return "group".equals(normalized) ? "group" : "private";
    }

    private String resolveConversationName(Conversation conversation, List<Long> participantIds, Long currentUserId) {
        if (conversation.getName() != null && !conversation.getName().isBlank()) {
            return conversation.getName();
        }
        if (!"private".equalsIgnoreCase(conversation.getType()) || participantIds == null || participantIds.isEmpty()) {
            return "未命名会话";
        }

        Long targetUserId = participantIds.stream()
                .filter(participantId -> !Objects.equals(participantId, currentUserId))
                .findFirst()
                .orElse(participantIds.get(0));
        return userInfoRepository.findById(targetUserId)
                .map(this::resolveUserName)
                .orElse("私聊");
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

    private Integer resolveUnreadCount(Long conversationId, List<ConversationParticipant> participants, Long currentUserId) {
        if (currentUserId == null || participants == null || participants.isEmpty()) {
            return 0;
        }

        Map<Long, ConversationParticipant> participantMap = participants.stream()
                .collect(Collectors.toMap(ConversationParticipant::getUserId, participant -> participant, (left, right) -> left));
        ConversationParticipant participant = participantMap.get(currentUserId);
        if (participant == null) {
            return 0;
        }
        return Math.toIntExact(messageRepository.countUnreadForUser(conversationId, currentUserId, participant.getLastReadAt()));
    }
}
