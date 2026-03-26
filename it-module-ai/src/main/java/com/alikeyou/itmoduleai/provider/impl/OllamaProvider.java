package com.alikeyou.itmoduleai.provider.impl;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.provider.model.AiProviderMessage;
import com.alikeyou.itmoduleai.provider.model.AiProviderStreamChunk;
import com.alikeyou.itmoduleai.provider.support.AiProviderParamResolver;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelOption;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OllamaProvider implements AiProvider {

    private final AiProviderParamResolver aiProviderParamResolver;
    private final ObjectMapper objectMapper;

    @Override
    public boolean supports(AiModel model) {
        return model != null
                && (model.getModelType() == AiModel.ModelType.OLLAMA
                || "ollama".equalsIgnoreCase(model.getProviderCode()));
    }

    @Override
    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/api/chat");
        Map<String, Object> body = buildBody(request);

        int timeoutMs = model != null && model.getTimeoutMs() != null ? model.getTimeoutMs() : 120000;
        RestClient restClient = buildRestClient(timeoutMs);

        String raw = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(String.class);

        return parseChatResponse(raw);
    }

    @Override
    public Flux<AiProviderStreamChunk> streamChat(AiProviderChatRequest request) {
        // 先保留“伪流式”实现，至少不超时、不返回整段 JSON
        AiProviderChatResponse response = chat(request);
        return Flux.just(
                AiProviderStreamChunk.builder()
                        .delta(response.getContent())
                        .finished(true)
                        .finishReason(response.getFinishReason())
                        .promptTokens(response.getPromptTokens())
                        .completionTokens(response.getCompletionTokens())
                        .totalTokens(response.getTotalTokens())
                        .build()
        );
    }

    private RestClient buildRestClient(int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        ReactorClientHttpRequestFactory requestFactory = new ReactorClientHttpRequestFactory(httpClient);
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    private AiProviderChatResponse parseChatResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);

            String content = null;
            JsonNode messageNode = root.path("message");
            if (!messageNode.isMissingNode()) {
                content = messageNode.path("content").asText(null);
            }

            String finishReason = root.path("done_reason").asText(null);

            Integer promptTokens = root.hasNonNull("prompt_eval_count")
                    ? root.get("prompt_eval_count").asInt()
                    : null;
            Integer completionTokens = root.hasNonNull("eval_count")
                    ? root.get("eval_count").asInt()
                    : null;
            Integer totalTokens = null;
            if (promptTokens != null || completionTokens != null) {
                totalTokens = (promptTokens == null ? 0 : promptTokens)
                        + (completionTokens == null ? 0 : completionTokens);
            }

            Integer latencyMs = root.hasNonNull("total_duration")
                    ? (int) (root.get("total_duration").asLong() / 1_000_000L)
                    : null;

            return AiProviderChatResponse.builder()
                    .content(content)
                    .rawResponse(raw)
                    .finishReason(finishReason)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .latencyMs(latencyMs)
                    .build();
        } catch (Exception e) {
            return AiProviderChatResponse.builder()
                    .content(raw)
                    .rawResponse(raw)
                    .finishReason("stop")
                    .errorCode("OLLAMA_PARSE_ERROR")
                    .build();
        }
    }

    private Map<String, Object> buildBody(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null ? null : model.getModelName());
        body.put("stream", false);
        body.putAll(aiProviderParamResolver.mergeParams(model, request.getRequestParams()));

        List<Map<String, Object>> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (AiProviderMessage item : request.getMessages()) {
                Map<String, Object> row = new LinkedHashMap<>();
                row.put("role", item.getRole());
                row.put("content", item.getContent());
                messages.add(row);
            }
        }
        body.put("messages", messages);
        return body;
    }

    private String normalizeEndpoint(String baseUrl, String suffix) {
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? "http://localhost:11434" : baseUrl;
        if (resolved.endsWith(suffix)) {
            return resolved;
        }
        if (resolved.endsWith("/")) {
            return resolved.substring(0, resolved.length() - 1) + suffix;
        }
        return resolved + suffix;
    }
}