package com.alikeyou.itmoduleai.provider.embedding;

import com.alikeyou.itmoduleai.provider.support.EmbeddingNameNormalizer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class OllamaEmbeddingProvider implements EmbeddingProvider {

    private final ObjectMapper objectMapper;

    @Value("${ai.embedding.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.embedding.http.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${ai.embedding.http.read-timeout-ms:120000}")
    private int readTimeoutMs;

    @Override
    public String providerCode() {
        return "ollama";
    }

    @Override
    public boolean supports(String providerCode) {
        return providerCode().equalsIgnoreCase(EmbeddingNameNormalizer.normalizeProvider(providerCode));
    }

    @Override
    public List<Double> embed(EmbeddingRequest request) {
        if (request == null || !StringUtils.hasText(request.getText())) {
            return List.of();
        }
        String modelName = EmbeddingNameNormalizer.normalizeModel(request.getModelName());
        if (!StringUtils.hasText(modelName)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing embedding model");
        }
        return requestOllamaEmbedding(request.getText(), modelName);
    }

    private List<Double> requestOllamaEmbedding(String text, String modelName) {
        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofMillis(Math.max(connectTimeoutMs, 1000)))
                    .build();
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", modelName);
            body.put("input", text);
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(trimTrailingSlash(ollamaBaseUrl) + "/api/embed"))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofMillis(Math.max(readTimeoutMs, 5000)))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                String message = trimError(extractErrorMessage(response.body()));
                throw new ResponseStatusException(
                        HttpStatus.BAD_GATEWAY,
                        "Ollama embedding request failed: " + response.statusCode()
                                + (StringUtils.hasText(message) ? (" - " + message) : "")
                );
            }
            JsonNode root = objectMapper.readTree(response.body());
            JsonNode embeddingsNode = root.get("embeddings");
            JsonNode vectorNode = null;
            if (embeddingsNode != null && embeddingsNode.isArray() && embeddingsNode.size() > 0) {
                vectorNode = embeddingsNode.get(0);
            }
            if (vectorNode == null) {
                JsonNode singleNode = root.get("embedding");
                if (singleNode != null && singleNode.isArray()) {
                    vectorNode = singleNode;
                }
            }
            if (vectorNode == null || !vectorNode.isArray()) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Ollama embedding response format is invalid");
            }
            List<Double> vector = new ArrayList<>(vectorNode.size());
            vectorNode.forEach(node -> vector.add(node.asDouble()));
            return vector;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            if (ex instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Ollama embedding request failed: " + trimError(ex.getMessage()),
                    ex
            );
        }
    }

    private String trimTrailingSlash(String raw) {
        String value = EmbeddingNameNormalizer.trimToNull(raw);
        if (value == null) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "unknown error";
        }
        String normalized = message.replace("\n", " ").replace("\r", " ").trim();
        return normalized.length() > 500 ? normalized.substring(0, 500) : normalized;
    }

    private String extractErrorMessage(String body) {
        if (!StringUtils.hasText(body)) {
            return null;
        }
        try {
            JsonNode root = objectMapper.readTree(body);
            if (root.hasNonNull("error")) {
                JsonNode node = root.get("error");
                if (node.isTextual()) {
                    return node.asText();
                }
                if (node.hasNonNull("message")) {
                    return node.get("message").asText();
                }
            }
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }
}
