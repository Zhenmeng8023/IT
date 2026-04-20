package com.alikeyou.itmoduleai.provider.embedding;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.provider.support.EmbeddingNameNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class EmbeddingProfileResolver {

    private final EmbeddingProviderManager embeddingProviderManager;

    @Value("${ai.embedding.default-provider:ollama}")
    private String defaultProvider;

    @Value("${ai.embedding.default-model:embeddinggemma:300m}")
    private String defaultModel;

    @Value("${ai.embedding.default-dimension:768}")
    private int defaultDimension;

    @Value("${ai.embedding.batch-size:25}")
    private int batchSize;

    public EmbeddingProfileInfo resolve(KnowledgeBase knowledgeBase,
                                        String source,
                                        String requestedProvider,
                                        String requestedModelName,
                                        Integer requestedDimension) {
        String configuredProvider = trimToNull(knowledgeBase == null ? null : knowledgeBase.getEmbeddingProvider());
        String configuredModelName = trimToNull(knowledgeBase == null ? null : knowledgeBase.getEmbeddingModel());
        String provider = firstText(requestedProvider, configuredProvider, defaultProvider);
        String modelName = firstText(requestedModelName, configuredModelName, defaultModel);
        Integer dimension = requestedDimension != null ? requestedDimension : defaultDimension;

        if (!StringUtils.hasText(provider)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing embedding provider");
        }
        if (!StringUtils.hasText(modelName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing embedding model");
        }

        String normalizedProvider = EmbeddingNameNormalizer.normalizeProvider(provider);
        String normalizedModelName = EmbeddingNameNormalizer.normalizeModel(modelName);
        Integer normalizedDimension = normalizeDimension(dimension);

        boolean providerSupported = embeddingProviderManager.isSupported(normalizedProvider);
        List<String> warningParts = new ArrayList<>();
        Set<String> sourceParts = new LinkedHashSet<>();

        if (StringUtils.hasText(requestedProvider) || StringUtils.hasText(requestedModelName) || requestedDimension != null) {
            sourceParts.add("REQUEST");
        }
        if (StringUtils.hasText(configuredProvider) || StringUtils.hasText(configuredModelName)) {
            sourceParts.add("KNOWLEDGE_BASE");
        }
        if (!StringUtils.hasText(configuredProvider) || !StringUtils.hasText(configuredModelName) || requestedDimension == null) {
            sourceParts.add("DEFAULT");
        }
        if (sourceParts.isEmpty()) {
            sourceParts.add("DEFAULT");
        }

        if (StringUtils.hasText(requestedProvider) && !Objects.equals(EmbeddingNameNormalizer.normalizeProvider(requestedProvider), configuredProvider)) {
            warningParts.add("request provider override applied");
        }
        if (StringUtils.hasText(requestedModelName) && !Objects.equals(EmbeddingNameNormalizer.normalizeModel(requestedModelName), configuredModelName)) {
            warningParts.add("request model override applied");
        }
        if (requestedDimension != null) {
            warningParts.add("request dimension override applied");
        }
        if (!StringUtils.hasText(configuredProvider) || !StringUtils.hasText(configuredModelName)) {
            warningParts.add("knowledge base embedding profile incomplete; using defaults");
        }
        if (!providerSupported) {
            warningParts.add("embedding provider is not registered: " + normalizedProvider);
        }

        return EmbeddingProfileInfo.builder()
                .requestedProvider(trimToNull(requestedProvider))
                .requestedModelName(trimToNull(requestedModelName))
                .requestedDimension(requestedDimension)
                .configuredProvider(configuredProvider)
                .configuredModelName(configuredModelName)
                .provider(normalizedProvider)
                .modelName(normalizedModelName)
                .dimension(normalizedDimension)
                .batchSize(Math.max(1, batchSize))
                .source(String.join("+", sourceParts))
                .providerSupported(providerSupported)
                .warning(warningParts.isEmpty() ? null : String.join("; ", warningParts))
                .build();
    }

    private Integer normalizeDimension(Integer dimension) {
        if (dimension == null) {
            return Math.max(32, Math.min(defaultDimension, 4096));
        }
        return Math.max(32, Math.min(dimension, 4096));
    }

    private String firstText(String... values) {
        if (values == null) {
            return null;
        }
        for (String value : values) {
            String normalized = trimToNull(value);
            if (StringUtils.hasText(normalized)) {
                return normalized;
            }
        }
        return null;
    }

    private String trimToNull(String value) {
        return EmbeddingNameNormalizer.trimToNull(value);
    }
}
