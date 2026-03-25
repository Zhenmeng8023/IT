package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleinteractive.entity.Conversation;
import com.alikeyou.itmoduleinteractive.entity.Message;

import java.util.List;
import java.util.Map;

/**
 * AI 会话服务接口
 */
public interface AiConversationService {

    /**
     * 创建 AI 会话
     * @param userId 用户 ID
     * @param aiType AI 类型（openai/baidu/qwen/deepseek 等）
     * @param aiModelName AI 模型名称
     * @param configParams AI 配置参数
     * @return 创建的会话
     */
    Conversation createAiConversation(Long userId, String aiType, String aiModelName, Map<String, Object> configParams);

    /**
     * 获取会话详情
     * @param conversationId 会话 ID
     * @return 会话信息
     */
    Conversation getConversation(Long conversationId);

    /**
     * 发送消息到 AI 会话
     * @param conversationId 会话 ID
     * @param userId 用户 ID
     * @param content 消息内容
     * @return AI 回复的消息
     */
    Message sendAiMessage(Long conversationId, Long userId, String content);

    /**
     * 获取会话消息列表
     * @param conversationId 会话 ID
     * @param page 页码
     * @param size 每页大小
     * @return 消息列表
     */
    List<Message> getConversationMessages(Long conversationId, int page, int size);

    /**
     * 清空会话上下文
     * @param conversationId 会话 ID
     */
    void clearConversation(Long conversationId);

    /**
     * 获取用户的 AI 会话列表
     * @param userId 用户 ID
     * @param page 页码
     * @param size 每页大小
     * @return 会话列表
     */
    List<Conversation> getUserAiConversations(Long userId, int page, int size);
}