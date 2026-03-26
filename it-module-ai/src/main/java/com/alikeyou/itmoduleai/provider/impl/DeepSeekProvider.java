package com.alikeyou.itmoduleai.provider.impl;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.provider.model.AiProviderMessage;
import com.alikeyou.itmoduleai.provider.model.AiProviderStreamChunk;
import com.alikeyou.itmoduleai.provider.support.AiProviderParamResolver;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DeepSeekProvider implements AiProvider {

    private final ObjectMapper objectMapper;
    private final AiProviderParamResolver aiProviderParamResolver;

    @Override
    public boolean supports(AiModel model) {
        return model != null && (model.getModelType() == AiModel.ModelType.DEEPSEEK || "deepseek".equalsIgnoreCase(model.getProviderCode()));
    }

    @Override
    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        Map<String, Object> body = buildBody(request);
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/chat/completions");

        String raw = RestClient.builder().build()
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + (model == null ? null : model.getApiKey()))
                .body(body)
                .retrieve()
                .body(String.class);

        return AiProviderChatResponse.builder()
                .content(raw)
                .rawResponse(raw)
                .finishReason("stop")
                .build();
    }

    @Override
    public Flux<AiProviderStreamChunk> streamChat(AiProviderChatRequest request) {
        return Flux.just(AiProviderStreamChunk.builder().delta(chat(request).getContent()).finished(true).finishReason("stop").build());
    }

    private Map<String, Object> buildBody(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model == null ? null : model.getModelName());
        body.put("stream", false);
        body.putAll(aiProviderParamResolver.mergeParams(model, request.getRequestParams()));

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
        return body;
    }

    private String normalizeEndpoint(String baseUrl, String suffix) {
        if (baseUrl == null || baseUrl.isBlank()) {
            return "https://api.deepseek.com" + suffix;
        }
        if (baseUrl.endsWith(suffix)) {
            return baseUrl;
        }
        if (baseUrl.endsWith("/")) {
            return baseUrl.substring(0, baseUrl.length() - 1) + suffix;
        }
        return baseUrl + suffix;
    }
}
