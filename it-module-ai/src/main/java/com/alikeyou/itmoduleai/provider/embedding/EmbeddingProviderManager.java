package com.alikeyou.itmoduleai.provider.embedding;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class EmbeddingProviderManager {

    private final List<EmbeddingProvider> providers;

    public EmbeddingProvider resolve(String providerCode) {
        return find(providerCode)
                .orElseThrow(() -> new IllegalStateException("No embedding provider available: " + providerCode));
    }

    public Optional<EmbeddingProvider> find(String providerCode) {
        return providers.stream()
                .filter(provider -> provider.supports(providerCode))
                .findFirst();
    }

    public boolean isSupported(String providerCode) {
        return find(providerCode).isPresent();
    }

    public List<String> providerCodes() {
        return providers.stream()
                .map(EmbeddingProvider::providerCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }
}
