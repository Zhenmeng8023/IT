package com.alikeyou.itmoduleai.provider.support;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AiProviderParamResolver {

    private final ObjectMapper objectMapper;

    public Map<String, Object> mergeParams(AiModel model, Map<String, Object> requestParams) {
        Map<String, Object> merged = new HashMap<>();
        if (model != null && model.getDefaultParams() != null && !model.getDefaultParams().isBlank()) {
            try {
                merged.putAll(objectMapper.readValue(model.getDefaultParams(), new TypeReference<Map<String, Object>>() {}));
            } catch (Exception ignored) {
            }
        }
        if (requestParams != null && !requestParams.isEmpty()) {
            merged.putAll(requestParams);
        }
        return merged;
    }

    public Map<String, Object> safeMap(Map<String, Object> requestParams) {
        return requestParams == null ? Collections.emptyMap() : requestParams;
    }
}
