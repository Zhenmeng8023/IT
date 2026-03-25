package com.alikeyou.itmodulecommon.service;

import reactor.core.publisher.Flux;
/**
 * AI 聊天服务接口
 */
public interface AiChatService {

    /**
     * 发送消息并获取 AI 回复
     * @param message 用户消息
     * @return AI 回复内容
     */
    String chat(String message);

    /**
     * 发送消息并获取 AI 回复（流式）
     * @param message 用户消息
     * @return AI 回复内容（流式）
     */
    Flux<String> chatStream(String message);

    /**
     * 检查 AI 服务是否可用
     * @return true-可用，false-不可用
     */
    boolean isAvailable();

    /**
     * 解释或翻译用户选中的内容
     * @param content 用户选中的内容
     * @param action 操作类型：explain(解释) 或 translate(翻译)
     * @return AI 处理结果
     */
    String explainOrTranslate(String content, String action);

    default Flux<String> explainOrTranslateStream(String content, String action) {
        return Flux.fromStream(java.util.stream.Stream.of(explainOrTranslate(content, action)));
    }

    /**
     * 解读代码
     * @param code 代码内容
     * @param language 编程语言（可选）
     * @return 代码解读结果
     */
    String explainCode(String code, String language);


    default Flux<String> explainCodeStream(String code, String language) {
        return Flux.fromStream(java.util.stream.Stream.of(explainCode(code, language)));
    }

    /**
     * 与 AI 进行交流（支持上下文）
     * @param message 用户消息
     * @param context 上下文历史
     * @return AI 回复
     */
    String chatWithContext(String message, String context);

    /**
     * 与 AI 进行交流（支持上下文，流式）
     * @param message 用户消息
     * @param context 上下文历史
     * @return AI 回复（流式）
     */
    Flux<String> chatWithContextStream(String message, String context);

    /**
     * 保存对话上下文
     * @param userId 用户 ID
     * @param message 用户消息
     * @param response AI 回复
     */
    void saveContext(String userId, String message, String response);

    /**
     * 获取对话上下文
     * */
    String getContext(String userId);

    /**
     * 清除对话上下文
     * @param userId 用户 ID
     */
    void clearContext(String userId);
}
