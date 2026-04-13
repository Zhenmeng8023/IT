package com.alikeyou.itmoduleai.provider.support;

import org.springframework.util.StringUtils;

import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;

public final class EmbeddingNameNormalizer {

    private EmbeddingNameNormalizer() {
    }

    public static String normalizeProvider(String provider) {
        String value = trimToNull(provider);
        if (value == null) {
            return null;
        }
        String normalized = stripDisplaySuffix(value).toLowerCase(Locale.ROOT);
        normalized = normalized.replaceAll("[\\s_]+", "-");
        return switch (normalized) {
            case "ollama-local", "local-ollama", "ollama-embedding", "ollama-embeddings" -> "ollama";
            case "openai-compatible", "openai-embedding", "openai-embeddings" -> "openai";
            default -> normalized;
        };
    }

    public static String normalizeModel(String modelName) {
        String value = trimToNull(modelName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String normalized = stripDisplaySuffix(value)
                .replaceAll("\\s+", "")
                .trim()
                .toLowerCase(Locale.ROOT);
        return normalized.isEmpty() ? null : normalized;
    }

    public static Set<String> providerLookupCandidates(String provider) {
        String normalized = normalizeProvider(provider);
        if (!StringUtils.hasText(normalized)) {
            return Set.of();
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
        candidates.add(normalized.replace("-", "_"));
        if ("ollama".equals(normalized)) {
            candidates.add("ollama-local");
            candidates.add("local-ollama");
            candidates.add("ollama_embedding");
            candidates.add("ollama-embedding");
        }
        if ("openai".equals(normalized)) {
            candidates.add("openai-compatible");
            candidates.add("openai_embedding");
            candidates.add("openai-embedding");
        }
        return candidates;
    }

    public static Set<String> modelLookupCandidates(String modelName) {
        String normalized = normalizeModel(modelName);
        if (!StringUtils.hasText(normalized)) {
            return Set.of();
        }
        LinkedHashSet<String> candidates = new LinkedHashSet<>();
        candidates.add(normalized);
        if (normalized.endsWith(":latest")) {
            candidates.add(normalized.substring(0, normalized.length() - ":latest".length()));
        } else if (!normalized.contains(":")) {
            candidates.add(normalized + ":latest");
        }
        return candidates;
    }

    public static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private static String stripDisplaySuffix(String value) {
        String current = value.trim();
        String previous;
        do {
            previous = current;
            current = current
                    .replaceAll("\\s*\\([^)]*\\)\\s*$", "")
                    .replaceAll("\\s*\\[[^]]*]\\s*$", "")
                    .replaceAll("\\s*\\{[^}]*}\\s*$", "")
                    .replaceAll("\\s*\\uFF08[^\\uFF09]*\\uFF09\\s*$", "")
                    .replaceAll("\\s*\\u3010[^\\u3011]*\\u3011\\s*$", "")
                    .replaceAll("\\s+-\\s*(embedding|embed|vector|xiangliang|qianru)\\s*$", "")
                    .trim();
        } while (!current.equals(previous));
        return current;
    }
}
