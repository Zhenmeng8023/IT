package com.alikeyou.itmoduleai.provider.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class EmbeddingProviderManager {

    private final List<EmbeddingProvider> providers;

    public EmbeddingProvider resolve(String providerCode) {
        return providers.stream()
                .filter(provider -> provider.supports(providerCode))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No embedding provider available: " + providerCode));
    }
}
