package com.alikeyou.itmodulecommon.service.impl;

import com.alikeyou.itmodulecommon.config.AIConfig;
import com.alikeyou.itmodulecommon.service.AiChatService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.ollama.OllamaChatModel;
import org.springframework.ai.ollama.api.OllamaApi;
import org.springframework.ai.ollama.api.OllamaChatOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;


import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class AiChatServiceImpl implements AiChatService {

    @Autowired
    private AIConfig aiConfig;

    private ChatModel chatModel;

    // 简单的上下文缓存（用户 ID -> 对话历史）
    private final ConcurrentHashMap<String, String> contextCache = new ConcurrentHashMap<>();

    @Override
    public String chat(String message) {
        try {
            if (!isAvailable()) {
                log.warn("AI 服务不可用");
                return "AI 服务暂时不可用，请稍后再试";
            }

            ChatClient chatClient = ChatClient.builder(getChatModel()).build();

            String response = chatClient.prompt()
                    .user(message)
                    .call()
                    .content();

            log.info("AI 回复：{}", response);
            return response;

        } catch (Exception e) {
            log.error("AI 聊天失败", e);
            return "抱歉，处理您的请求时出现错误";
        }
    }

    @Override
    public Flux<String> chatStream(String message) {
        try {
            if (!isAvailable()) {
                log.warn("AI 服务不可用");
                return Flux.just("AI 服务暂时不可用，请稍后再试");
            }

            ChatClient chatClient = ChatClient.builder(getChatModel()).build();

            return chatClient.prompt()
                    .user(message)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("AI 流式聊天失败", e);
            return Flux.error(e);
        }
    }


    @Override
    public boolean isAvailable() {
        return aiConfig.getOllama() != null && aiConfig.getOllama().isEnabled();
    }

    @Override
    public String explainOrTranslate(String content, String action) {
        if (content == null || content.trim().isEmpty()) {
            return "内容为空，无法处理";
        }

        try {
            String prompt;
            if ("translate".equals(action)) {
                // 检测是否为英文，如果是则翻译成中文，否则翻译成英文
                prompt = String.format(
                        "请判断以下文本的语言。如果是英语，请翻译成中文；如果是其他语言，请翻译成英语。\n\n" +
                                "文本内容：%s\n\n" +
                                "只需返回翻译结果，不需要解释。",
                        content
                );
            } else {
                // 默认进行解释
                prompt = String.format(
                        "请对以下内容进行详细解释，帮助我理解：\n\n%s\n\n" +
                                "请用清晰、易懂的语言解释，必要时可以举例说明。",
                        content
                );
            }

            return chat(prompt);

        } catch (Exception e) {
            log.error("解释/翻译失败", e);
            return "处理失败：" + e.getMessage();
        }
    }

    @Override
    public String explainCode(String code, String language) {
        if (code == null || code.trim().isEmpty()) {
            return "代码为空，无法解读";
        }

        try {
            String langInfo = language != null && !language.isEmpty()
                    ? language + "语言"
                    : "这段";

            String prompt = String.format(
                    "请详细解读以下%s代码:\n" +
                            "```%s\n" +
                            "%s\n" +
                            "```\n\n" +
                            "请从以下几个方面分析：\n" +
                            "1. 这段代码的主要功能和作用\n" +
                            "2. 关键部分的工作原理\n" +
                            "3. 使用了哪些重要的技术或设计模式\n" +
                            "4. 可能的使用场景\n\n" +
                            "请用简洁清晰的语言说明。",
                    langInfo,
                    language != null ? language : "",
                    code
            );

            return chat(prompt);

        } catch (Exception e) {
            log.error("代码解读失败", e);
            return "代码解读失败：" + e.getMessage();
        }
    }

    @Override
    public String chatWithContext(String message, String context) {
        try {
            if (!isAvailable()) {
                return "AI 服务暂时不可用，请稍后再试";
            }

            ChatClient chatClient = ChatClient.builder(getChatModel()).build();

            // 如果有上下文，将其加入提示
            String promptText = message;
            if (context != null && !context.isEmpty()) {
                promptText = String.format(
                        "对话历史：\n%s\n\n" +
                                "用户：%s\n\n" +
                                "请根据上面的对话历史回答用户的问题：",
                        context,
                        message
                );
            }

            String response = chatClient.prompt()
                    .user(promptText)
                    .call()
                    .content();

            log.info("AI 回复（带上下文）: {}", response);
            return response;

        } catch (Exception e) {
            log.error("AI 聊天失败（带上下文）", e);
            return "抱歉，处理您的请求时出现错误";
        }
    }

    @Override
    public Flux<String> chatWithContextStream(String message, String context) {
        try {
            if (!isAvailable()) {
                return Flux.just("AI 服务暂时不可用，请稍后再试");
            }

            ChatClient chatClient = ChatClient.builder(getChatModel()).build();

            // 如果有上下文，将其加入提示
            String promptText = message;
            if (context != null && !context.isEmpty()) {
                promptText = String.format(
                        "对话历史：\n%s\n\n" +
                                "用户：%s\n\n" +
                                "请根据上面的对话历史回答用户的问题：",
                        context,
                        message
                );
            }

            return chatClient.prompt()
                    .user(promptText)
                    .stream()
                    .content();

        } catch (Exception e) {
            log.error("AI 流式聊天失败（带上下文）", e);
            return Flux.error(e);
        }
    }

    /**
     * 保存对话上下文
     */
    public void saveContext(String userId, String message, String response) {
        String history = contextCache.getOrDefault(userId, "");
        String newHistory = String.format(
                "%s用户：%s\nAI：%s\n\n",
                history,
                message,
                response
        );

        // 限制上下文长度，只保留最近的 10 轮对话
        if (newHistory.length() > 5000) {
            String[] lines = newHistory.split("\n\n");
            newHistory = String.join("\n\n",
                    java.util.Arrays.copyOfRange(lines, Math.max(0, lines.length - 20), lines.length));
        }

        contextCache.put(userId, newHistory);
    }

    /**
     * 获取对话上下文
     */
    public String getContext(String userId) {
        return contextCache.getOrDefault(userId, "");
    }

    /**
     * 清除对话上下文
     */
    public void clearContext(String userId) {
        contextCache.remove(userId);
    }

    private ChatModel getChatModel() {
        if (chatModel == null) {
            AIConfig.OllamaConfig ollamaConfig = aiConfig.getOllama();
            OllamaApi ollamaApi = OllamaApi.builder()
                    .baseUrl(ollamaConfig.getBaseUrl())
                    .build();

            OllamaChatOptions options = new OllamaChatOptions();
            options.setModel(ollamaConfig.getChat().getModel());

            if (ollamaConfig.getChat().getTemperature() != null) {
                options.setTemperature(ollamaConfig.getChat().getTemperature().doubleValue());
            }
            if (ollamaConfig.getChat().getMaxTokens() != null) {
                options.setMaxTokens(ollamaConfig.getChat().getMaxTokens());
            }

            chatModel = OllamaChatModel.builder()
                    .ollamaApi(ollamaApi)
                    .defaultOptions(options)
                    .build();
        }

        return chatModel;
    }
}