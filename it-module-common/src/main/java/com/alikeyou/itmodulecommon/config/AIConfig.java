package com.alikeyou.itmodulecommon.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * AI 配置类
 * 用于读取 application-ai.yml 中的配置
 */
@Configuration
@ConfigurationProperties(prefix = "spring.ai.ollama")
@Data
public class AIConfig {

    /**
     * Ollama 服务地址
     */
    private String baseUrl;

    /**
     * 聊天功能配置
     */
    private ChatOptions chat;

    /**
     * 检查 Ollama 配置是否可用
     */
    public boolean isEnabled() {
        return baseUrl != null && !baseUrl.isEmpty()
                && chat != null;
    }

    /**
     * 聊天功能配置类
     */
    @Data
    public static class ChatOptions {

        /**
         * 模型名称
         */
        private String model = "qwen3.5:latest";

        /**
         * 温度参数 (0-1)
         */
        private Float temperature;

        /**
         * 最大输出令牌数
         */
        private Integer maxTokens;

        /**
         * 是否流式返回结果
         */
        private Boolean stream = false;

        /**
         * 是否启用聊天功能（默认 true）
         */
        private boolean enable = true;

        /**
         * 获取完整的选项 Map（如果需要传递给 API）
         */
        public java.util.Map<String, Object> getOptionsAsMap() {
            java.util.Map<String, Object> options = new java.util.HashMap<>();
            if (temperature != null) {
                options.put("temperature", temperature);
            }
            if (maxTokens != null) {
                options.put("max_tokens", maxTokens);
            }
            return options;
        }
    }
}