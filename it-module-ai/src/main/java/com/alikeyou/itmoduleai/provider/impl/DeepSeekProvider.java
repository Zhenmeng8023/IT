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
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.http.client.ReactorClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.http.codec.ServerSentEvent;
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
import java.util.Objects;

@Component
@RequiredArgsConstructor
public class DeepSeekProvider implements AiProvider {

    private final ObjectMapper objectMapper;
    private final AiProviderParamResolver aiProviderParamResolver;

    @Override
    public boolean supports(AiModel model) {
        return model != null
                && (model.getModelType() == AiModel.ModelType.DEEPSEEK
                || "deepseek".equalsIgnoreCase(model.getProviderCode()));
    }

    @Override
    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/chat/completions");
        int timeoutMs = model != null && model.getTimeoutMs() != null ? model.getTimeoutMs() : 120000;

        RestClient restClient = buildRestClient(timeoutMs);
        String raw = restClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.APPLICATION_JSON)
                .header("Authorization", bearerToken(model))
                .body(buildBody(request, false))
                .retrieve()
                .body(String.class);

        return parseChatResponse(raw);
    }

    @Override
    public Flux<AiProviderStreamChunk> streamChat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/chat/completions");
        int timeoutMs = model != null && model.getTimeoutMs() != null ? model.getTimeoutMs() : 120000;

        WebClient webClient = buildWebClient(timeoutMs);

        return webClient.post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM, MediaType.APPLICATION_JSON)
                .header("Authorization", bearerToken(model))
                .bodyValue(buildBody(request, true))
                .retrieve()
                .bodyToFlux(new ParameterizedTypeReference<ServerSentEvent<String>>() {})
                .map(ServerSentEvent::data)
                .filter(Objects::nonNull)
                .takeUntil("[DONE]"::equals)
                .filter(data -> !"[DONE]".equals(data))
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
                .requestFactory(new ReactorClientHttpRequestFactory(httpClient))
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

            JsonNode choice = firstChoice(root);
            JsonNode message = choice.path("message");
            JsonNode usage = root.path("usage");

            String content = readText(message, "content");
            if (content == null) {
                content = readText(message, "reasoning_content");
            }

            Integer promptTokens = usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer completionTokens = usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;

            return AiProviderChatResponse.builder()
                    .content(content)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .finishReason(readText(choice, "finish_reason"))
                    .rawResponse(raw)
                    .build();
        } catch (Exception e) {
            return AiProviderChatResponse.builder()
                    .content(raw)
                    .rawResponse(raw)
                    .finishReason("stop")
                    .errorCode("DEEPSEEK_PARSE_ERROR")
                    .build();
        }
    }

    private AiProviderStreamChunk parseStreamChunk(String data) {
        try {
            JsonNode root = objectMapper.readTree(data);

            JsonNode choice = firstChoice(root);
            JsonNode deltaNode = choice.path("delta");
            JsonNode usage = root.path("usage");

            String delta = readText(deltaNode, "content");
            if (delta == null) {
                delta = readText(deltaNode, "reasoning_content");
            }

            String finishReason = readText(choice, "finish_reason");
            Boolean finished = finishReason != null && !finishReason.isBlank();

            Integer promptTokens = usage.hasNonNull("prompt_tokens") ? usage.get("prompt_tokens").asInt() : null;
            Integer completionTokens = usage.hasNonNull("completion_tokens") ? usage.get("completion_tokens").asInt() : null;
            Integer totalTokens = usage.hasNonNull("total_tokens") ? usage.get("total_tokens").asInt() : null;

            return AiProviderStreamChunk.builder()
                    .delta(delta)
                    .finished(finished)
                    .finishReason(finishReason)
                    .promptTokens(promptTokens)
                    .completionTokens(completionTokens)
                    .totalTokens(totalTokens)
                    .build();
        } catch (Exception e) {
            return AiProviderStreamChunk.builder()
                    .delta(data)
                    .finished(false)
                    .finishReason(null)
                    .build();
        }
    }

    private JsonNode firstChoice(JsonNode root) {
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            return choices.get(0);
        }
        return objectMapper.createObjectNode();
    }

    private String readText(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode()) {
            return null;
        }
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isEmpty() ? null : text;
    }

    private Map<String, Object> buildBody(AiProviderChatRequest request, boolean stream) {
        AiModel model = request.getModel();

        Map<String, Object> body = new LinkedHashMap<>();
        body.putAll(aiProviderParamResolver.mergeParams(model, request.getRequestParams()));

        body.put("model", model == null ? null : model.getModelName());
        body.put("stream", stream);

        List<Map<String, String>> messages = new ArrayList<>();
        if (request.getMessages() != null) {
            for (AiProviderMessage item : request.getMessages()) {
                Map<String, String> row = new LinkedHashMap<>();
                row.put("role", item.getRole());
                row.put("content", item.getContent());
                messages.add(row);
            }
        }
        body.put("messages", messages);

        if (stream) {
            Map<String, Object> streamOptions = new LinkedHashMap<>();
            streamOptions.put("include_usage", true);
            body.put("stream_options", streamOptions);
        } else {
            body.remove("stream_options");
        }

        return body;
    }

    private String normalizeEndpoint(String baseUrl, String suffix) {
        String resolved = (baseUrl == null || baseUrl.isBlank()) ? "https://api.deepseek.com" : baseUrl;

        if (resolved.endsWith(suffix)) {
            return resolved;
        }
        if (resolved.endsWith("/")) {
            return resolved.substring(0, resolved.length() - 1) + suffix;
        }
        return resolved + suffix;
    }

    private String bearerToken(AiModel model) {
        return "Bearer " + (model == null ? null : model.getApiKey());
    }
}