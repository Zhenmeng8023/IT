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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.reactive.function.client.WebClient;
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

    @Value("${ai.provider.ollama.base-url:http://localhost:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.provider.default-timeout-ms:120000}")
    private int defaultTimeoutMs;

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
        int timeoutMs = resolveTimeoutMs(model);

        RestClient restClient = buildRestClient(timeoutMs);
        String raw = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .body(buildBody(request, false))
                .retrieve()
                .body(String.class);

        return parseChatResponse(raw);
    }

    @Override
    public Flux<AiProviderStreamChunk> streamChat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/api/chat");
        int timeoutMs = resolveTimeoutMs(model);

        WebClient webClient = buildWebClient(timeoutMs);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_NDJSON, MediaType.APPLICATION_JSON)
                .bodyValue(buildBody(request, true))
                .retrieve()
                .bodyToFlux(JsonNode.class)
                .map(this::parseStreamChunk)
                .filter(chunk ->
                        chunk.getDelta() != null
                                || Boolean.TRUE.equals(chunk.getFinished())
                                || chunk.getPromptTokens() != null
                                || chunk.getCompletionTokens() != null
                                || chunk.getTotalTokens() != null
                                || chunk.getFinishReason() != null
                );
    }

    private RestClient buildRestClient(int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return RestClient.builder()
                .requestFactory(new org.springframework.http.client.ReactorClientHttpRequestFactory(httpClient))
                .build();
    }

    private WebClient buildWebClient(int timeoutMs) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
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
            Integer promptTokens = root.hasNonNull("prompt_eval_count") ? root.get("prompt_eval_count").asInt() : null;
            Integer completionTokens = root.hasNonNull("eval_count") ? root.get("eval_count").asInt() : null;

            Integer totalTokens = null;
            if (promptTokens != null || completionTokens != null) {
                totalTokens = (promptTokens == null ? 0 : promptTokens) + (completionTokens == null ? 0 : completionTokens);
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

    private AiProviderStreamChunk parseStreamChunk(JsonNode root) {
        String delta = null;
        JsonNode messageNode = root.path("message");
        if (!messageNode.isMissingNode()) {
            delta = messageNode.path("content").asText(null);
            if (delta != null && delta.isEmpty()) {
                delta = null;
            }
        }

        Boolean finished = root.has("done") ? root.get("done").asBoolean(false) : false;
        String finishReason = root.path("done_reason").asText(null);

        Integer promptTokens = root.hasNonNull("prompt_eval_count") ? root.get("prompt_eval_count").asInt() : null;
        Integer completionTokens = root.hasNonNull("eval_count") ? root.get("eval_count").asInt() : null;

        Integer totalTokens = null;
        if (promptTokens != null || completionTokens != null) {
            totalTokens = (promptTokens == null ? 0 : promptTokens) + (completionTokens == null ? 0 : completionTokens);
        }

        return AiProviderStreamChunk.builder()
                .delta(delta)
                .finished(finished)
                .finishReason(finishReason)
                .promptTokens(promptTokens)
                .completionTokens(completionTokens)
                .totalTokens(totalTokens)
                .build();
    }

    private Map<String, Object> buildBody(AiProviderChatRequest request, boolean stream) {
        AiModel model = request.getModel();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null ? null : model.getModelName());
        body.put("stream", stream);
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
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? ollamaBaseUrl : baseUrl;

        if (resolved.endsWith(suffix)) {
            return resolved;
        }
        if (resolved.endsWith("/")) {
            return resolved.substring(0, resolved.length() - 1) + suffix;
        }
        return resolved + suffix;
    }

    private int resolveTimeoutMs(AiModel model) {
        int fallback = defaultTimeoutMs < 1000 ? 120000 : defaultTimeoutMs;
        if (model == null || model.getTimeoutMs() == null || model.getTimeoutMs() < 1000) {
            return fallback;
        }
        return model.getTimeoutMs();
    }
}
