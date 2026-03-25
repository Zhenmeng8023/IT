package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.service.AiChatService;
import com.alikeyou.itmoduleai.service.AiConversationService;
import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.entity.Message;
import com.alikeyou.itmoduleinteractive.repository.ConversationRepository;
import com.alikeyou.itmoduleinteractive.repository.MessageRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@Transactional
public class AiConversationServiceImpl implements AiConversationService {

    @Autowired
    private ConversationRepository conversationRepository;

    @Autowired
    private MessageRepository messageRepository;

    @Autowired
    private AiChatService aiChatService;

    @Override
    public Conversation createAiConversation(Long userId, String aiType, String aiModelName, Map<String, Object> configParams) {
        Conversation conversation = new Conversation();
        conversation.setType("private");
        conversation.setCreatorId(userId);
        conversation.setAiType(aiType);
        conversation.setAiModelName(aiModelName);
        conversation.setAiConfigParams(configParams != null ? configParams : new HashMap<>());
        conversation.setName("AI 对话-" + System.currentTimeMillis());
        conversation.setCreatedAt(Instant.now());
        conversation.setUpdatedAt(Instant.now());

        Conversation saved = conversationRepository.save(conversation);
        log.info("创建 AI 会话成功：{}, 用户：{}, AI 类型：{}", saved.getId(), userId, aiType);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Conversation getConversation(Long conversationId) {
        return conversationRepository.findById(conversationId)
                .orElseThrow(() -> new RuntimeException("会话不存在"));
    }

    @Override
    public Message sendAiMessage(Long conversationId, Long userId, String content) {
        // 1. 保存用户消息
        Message userMessage = new Message();
        userMessage.setConversationId(conversationId);
        userMessage.setSenderId(userId);
        userMessage.setContent(content);
        userMessage.setMessageType("text");
        userMessage.setSentAt(Instant.now());
        userMessage.setIsRead(true);
        userMessage.setIsAiResponse(false);

        Message savedUserMessage = messageRepository.save(userMessage);

        // 2. 获取会话信息
        Conversation conversation = getConversation(conversationId);

        // 3. 调用 AI 服务获取回复
        String aiResponse;
        try {
            if (!aiChatService.isAvailable()) {
                aiResponse = "AI 服务暂时不可用，请稍后再试";
            } else {
                aiResponse = aiChatService.chat(content);
            }
        } catch (Exception e) {
            log.error("AI 回复失败：{}", e.getMessage(), e);
            aiResponse = "抱歉，处理您的请求时出现错误：" + e.getMessage();
        }

        // 4. 保存 AI 回复
        Message aiMessage = new Message();
        aiMessage.setConversationId(conversationId);
        aiMessage.setSenderId(userId); // AI 消息的发送者也设为用户 ID（或者可以设为特殊值）
        aiMessage.setContent(aiResponse);
        aiMessage.setMessageType("text");
        aiMessage.setSentAt(Instant.now());
        aiMessage.setIsRead(true);
        aiMessage.setIsAiResponse(true);
        aiMessage.setAiModelUsed(conversation.getAiModelName());

        Message savedAiMessage = messageRepository.save(aiMessage);

        // 5. 更新会话时间
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);

        log.info("AI 会话消息：会话 ID={}, 用户 ID={}, AI 回复长度={}", conversationId, userId, aiResponse.length());
        return savedAiMessage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Message> getConversationMessages(Long conversationId, int page, int size) {
        // TODO: 实现分页查询
        return messageRepository.findAll().stream()
                .filter(m -> m.getConversationId().equals(conversationId))
                .toList();
    }

    @Override
    public void clearConversation(Long conversationId) {
        Conversation conversation = getConversation(conversationId);
        conversation.setUpdatedAt(Instant.now());
        conversationRepository.save(conversation);
        log.info("清空会话上下文：{}", conversationId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Conversation> getUserAiConversations(Long userId, int page, int size) {
        // TODO: 实现分页查询
        return conversationRepository.findAll().stream()
                .filter(c -> c.getCreatorId().equals(userId) && c.getAiType() != null)
                .toList();
    }
}