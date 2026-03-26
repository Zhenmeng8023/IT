package com.alikeyou.itmoduleai.provider.impl;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.provider.AiProvider;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatRequest;
import com.alikeyou.itmoduleai.provider.model.AiProviderChatResponse;
import com.alikeyou.itmoduleai.provider.model.AiProviderMessage;
import com.alikeyou.itmoduleai.provider.model.AiProviderStreamChunk;
import com.alikeyou.itmoduleai.provider.support.AiProviderParamResolver;
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
public class OllamaProvider implements AiProvider {

    private final AiProviderParamResolver aiProviderParamResolver;

    @Override
    public boolean supports(AiModel model) {
        return model != null && (model.getModelType() == AiModel.ModelType.OLLAMA || "ollama".equalsIgnoreCase(model.getProviderCode()));
    }

    @Override
    public AiProviderChatResponse chat(AiProviderChatRequest request) {
        AiModel model = request.getModel();
        String endpoint = normalizeEndpoint(model == null ? null : model.getBaseUrl(), "/api/chat");
        Map<String, Object> body = buildBody(request);

        String raw = RestClient.builder().build()
                .post()
                .uri(endpoint)
                .contentType(MediaType.APPLICATION_JSON)
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
