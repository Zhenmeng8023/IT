package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingServiceImpl implements KnowledgeEmbeddingService {

    private static final int DEFAULT_BATCH_SIZE = 25;
    private static final int DEFAULT_DIMENSION = 768;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;

    @Value("${ai.embedding.default-provider:ollama}")
    private String defaultProvider;

    @Value("${ai.embedding.default-model:embeddinggemma:300m}")
    private String defaultModel;

    @Value("${ai.embedding.ollama.base-url:http://127.0.0.1:11434}")
    private String ollamaBaseUrl;

    @Value("${ai.embedding.http.connect-timeout-ms:5000}")
    private int connectTimeoutMs;

    @Value("${ai.embedding.http.read-timeout-ms:120000}")
    private int readTimeoutMs;

    private final ConcurrentMap<String, ReentrantLock> runningLocks = new ConcurrentHashMap<>();

    @Override
    public KnowledgeEmbeddingStatusResponse backfillDocumentEmbeddings(Long documentId, String provider, String modelName, Integer dimension) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识文档不存在"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
        if (chunks.isEmpty()) {
            return buildDocumentStatus(documentId, document.getKnowledgeBase(), provider, modelName, dimension, 0L, 0L);
        }
        KnowledgeBase knowledgeBase = chunks.get(0).getKnowledgeBase();
        String actualProvider = resolveProvider(provider, knowledgeBase);
        String actualModel = resolveModel(modelName, knowledgeBase);
        String lockKey = "DOCUMENT:" + documentId + ":" + actualProvider + ":" + actualModel;
        ReentrantLock lock = runningLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResponseStatusException(CONFLICT, "当前文档已有向量回填任务正在执行");
        }
        try {
            long createdCount = 0L;
            for (List<KnowledgeChunk> batch : partition(chunks, DEFAULT_BATCH_SIZE)) {
                createdCount += writeEmbeddingBatch(batch, actualProvider, actualModel, normalizeDimension(dimension));
            }
            long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByDocumentId(documentId);
            return buildDocumentStatus(documentId, knowledgeBase, actualProvider, actualModel, dimension, embeddedCount, createdCount);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public KnowledgeEmbeddingStatusResponse backfillKnowledgeBaseEmbeddings(Long knowledgeBaseId, String provider, String modelName, Integer dimension) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识库不存在"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId);
        if (chunks.isEmpty()) {
            return buildKnowledgeBaseStatus(knowledgeBaseId, knowledgeBase, provider, modelName, dimension, 0L, 0L);
        }
        String actualProvider = resolveProvider(provider, knowledgeBase);
        String actualModel = resolveModel(modelName, knowledgeBase);
        String lockKey = "KB:" + knowledgeBaseId + ":" + actualProvider + ":" + actualModel;
        ReentrantLock lock = runningLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResponseStatusException(CONFLICT, "当前知识库已有向量回填任务正在执行");
        }
        try {
            long createdCount = 0L;
            for (List<KnowledgeChunk> batch : partition(chunks, DEFAULT_BATCH_SIZE)) {
                createdCount += writeEmbeddingBatch(batch, actualProvider, actualModel, normalizeDimension(dimension));
            }
            long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByKnowledgeBaseId(knowledgeBaseId);
            return buildKnowledgeBaseStatus(knowledgeBaseId, knowledgeBase, actualProvider, actualModel, dimension, embeddedCount, createdCount);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Double> embedText(String text, String provider, String modelName, Integer dimension) {
        String actualProvider = normalizeProvider(provider);
        String actualModel = normalizeModel(modelName);
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        if (!"ollama".equalsIgnoreCase(actualProvider)) {
            throw new ResponseStatusException(BAD_REQUEST, "当前仅支持 ollama 作为 embedding provider");
        }
        return requestOllamaEmbedding(text, actualModel);
    }

    @Override
    public List<Double> parseVectorPayload(String vectorPayload) {
        if (!StringUtils.hasText(vectorPayload)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(vectorPayload, new TypeReference<List<Double>>() {});
        } catch (Exception e) {
            return List.of();
        }
    }

    public KnowledgeEmbeddingStatusResponse getKnowledgeBaseEmbeddingStatus(Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识库不存在"));
        long totalChunkCount = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId).size();
        long embeddedChunkCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByKnowledgeBaseId(knowledgeBaseId);
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("KNOWLEDGE_BASE")
                .targetId(knowledgeBaseId)
                .totalChunkCount(totalChunkCount)
                .embeddedChunkCount(embeddedChunkCount)
                .createdEmbeddingCount(0L)
                .provider(resolveProvider(null, kb))
                .modelName(resolveModel(null, kb))
                .dimension(DEFAULT_DIMENSION)
                .build();
    }

    public KnowledgeEmbeddingStatusResponse getDocumentEmbeddingStatus(Long documentId) {
        KnowledgeDocument doc = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识文档不存在"));
        KnowledgeBase kb = doc.getKnowledgeBase();
        long totalChunkCount = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId).size();
        long embeddedChunkCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByDocumentId(documentId);
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("DOCUMENT")
                .targetId(documentId)
                .totalChunkCount(totalChunkCount)
                .embeddedChunkCount(embeddedChunkCount)
                .createdEmbeddingCount(0L)
                .provider(resolveProvider(null, kb))
                .modelName(resolveModel(null, kb))
                .dimension(DEFAULT_DIMENSION)
                .build();
    }

    private KnowledgeEmbeddingStatusResponse buildKnowledgeBaseStatus(Long knowledgeBaseId,
                                                                      KnowledgeBase knowledgeBase,
                                                                      String provider,
                                                                      String modelName,
                                                                      Integer dimension,
                                                                      long embeddedCount,
                                                                      long createdCount) {
        long totalChunkCount = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId).size();
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("KNOWLEDGE_BASE")
                .targetId(knowledgeBaseId)
                .totalChunkCount(totalChunkCount)
                .embeddedChunkCount(embeddedCount)
                .createdEmbeddingCount(createdCount)
                .provider(resolveProvider(provider, knowledgeBase))
                .modelName(resolveModel(modelName, knowledgeBase))
                .dimension(normalizeDimension(dimension))
                .build();
    }

    private KnowledgeEmbeddingStatusResponse buildDocumentStatus(Long documentId,
                                                                 KnowledgeBase knowledgeBase,
                                                                 String provider,
                                                                 String modelName,
                                                                 Integer dimension,
                                                                 long embeddedCount,
                                                                 long createdCount) {
        long totalChunkCount = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId).size();
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("DOCUMENT")
                .targetId(documentId)
                .totalChunkCount(totalChunkCount)
                .embeddedChunkCount(embeddedCount)
                .createdEmbeddingCount(createdCount)
                .provider(resolveProvider(provider, knowledgeBase))
                .modelName(resolveModel(modelName, knowledgeBase))
                .dimension(normalizeDimension(dimension))
                .build();
    }

    private long writeEmbeddingBatch(List<KnowledgeChunk> batch, String provider, String modelName, Integer fallbackDimension) {
        if (batch == null || batch.isEmpty()) {
            return 0L;
        }
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Long value = tx.execute(status -> {
            long count = 0L;
            for (KnowledgeChunk chunk : batch) {
                if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
                    continue;
                }
                List<Double> vector = embedText(buildEmbeddingInput(chunk), provider, modelName, fallbackDimension);
                int actualDimension = vector.isEmpty() ? normalizeDimension(fallbackDimension) : vector.size();
                Optional<KnowledgeChunkEmbedding> existingOpt = knowledgeChunkEmbeddingRepository
                        .findFirstByChunk_IdAndProviderCodeAndModelName(chunk.getId(), provider, modelName);
                KnowledgeChunkEmbedding embedding = existingOpt.orElseGet(KnowledgeChunkEmbedding::new);
                if (embedding.getId() == null) {
                    embedding.setChunk(chunk);
                    embedding.setProviderCode(provider);
                    embedding.setModelName(modelName);
                }
                embedding.setDimension(actualDimension);
                embedding.setVectorPayload(toJson(vector));
                embedding.setVectorRef(buildVectorRef(chunk.getId(), provider, modelName));
                embedding.setStatus(KnowledgeChunkEmbedding.Status.ACTIVE);
                KnowledgeChunkEmbedding saved = knowledgeChunkEmbeddingRepository.save(embedding);

                chunk.setEmbeddingProvider(provider);
                chunk.setEmbeddingModel(modelName);
                chunk.setVectorId(StringUtils.hasText(saved.getVectorRef()) ? saved.getVectorRef() : String.valueOf(saved.getId()));
                knowledgeChunkRepository.save(chunk);
                count++;
            }
            return count;
        });
        return value == null ? 0L : value;
    }

    private String buildEmbeddingInput(KnowledgeChunk chunk) {
        StringBuilder sb = new StringBuilder();
        KnowledgeDocument document = chunk.getDocument();
        if (document != null) {
            if (StringUtils.hasText(document.getFileName())) {
                sb.append("文件名: ")
                        .append(document.getFileName().trim())
                        .append("\n");
            }
            if (StringUtils.hasText(document.getTitle())) {
                sb.append("标题: ")
                        .append(document.getTitle().trim())
                        .append("\n");
            }
        }
        sb.append("Chunk: ")
                .append(chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex())
                .append("\n");
        sb.append("内容:\n")
                .append(chunk.getContent().trim());
        return sb.toString();
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
                throw new ResponseStatusException(BAD_GATEWAY, "调用 Ollama Embedding 失败: " + response.statusCode() + (StringUtils.hasText(message) ? (" - " + message) : ""));
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
                throw new ResponseStatusException(BAD_GATEWAY, "Ollama Embedding 返回格式不正确");
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
            throw new ResponseStatusException(BAD_GATEWAY, "调用 Ollama Embedding 失败: " + trimError(ex.getMessage()), ex);
        }
    }

    private String resolveProvider(String provider, KnowledgeBase knowledgeBase) {
        String resolved = trimToNull(provider);
        if (!StringUtils.hasText(resolved) && knowledgeBase != null) {
            resolved = trimToNull(knowledgeBase.getEmbeddingProvider());
        }
        if (!StringUtils.hasText(resolved)) {
            resolved = trimToNull(defaultProvider);
        }
        if (!StringUtils.hasText(resolved)) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少 embedding provider，请先在知识库里配置 Embedding Provider");
        }
        return normalizeProvider(resolved);
    }

    private String resolveModel(String modelName, KnowledgeBase knowledgeBase) {
        String resolved = trimToNull(modelName);
        if (!StringUtils.hasText(resolved) && knowledgeBase != null) {
            resolved = trimToNull(knowledgeBase.getEmbeddingModel());
        }
        if (!StringUtils.hasText(resolved)) {
            resolved = trimToNull(defaultModel);
        }
        if (!StringUtils.hasText(resolved)) {
            throw new ResponseStatusException(BAD_REQUEST, "缺少 embedding model，请先在知识库里配置 Embedding Model");
        }
        return normalizeModel(resolved);
    }

    private String normalizeProvider(String provider) {
        String value = trimToNull(provider);
        return value == null ? null : value.toLowerCase(Locale.ROOT);
    }

    private String normalizeModel(String modelName) {
        String value = trimToNull(modelName);
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.replaceAll("\\s*[（(][^）)]*[）)]\\s*$", "").trim();
    }

    private Integer normalizeDimension(Integer dimension) {
        if (dimension == null) {
            return DEFAULT_DIMENSION;
        }
        return Math.max(32, Math.min(dimension, 4096));
    }

    private String buildVectorRef(Long chunkId, String providerCode, String modelName) {
        return providerCode + ":" + modelName + ":" + chunkId + ":" + Instant.now().toEpochMilli();
    }

    private String toJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String trimTrailingSlash(String raw) {
        String value = trimToNull(raw);
        if (value == null) {
            return null;
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }


    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "未知错误";
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
                if (node.isTextual()) return node.asText();
                if (node.hasNonNull("message")) return node.get("message").asText();
            }
            if (root.hasNonNull("message")) {
                return root.get("message").asText();
            }
        } catch (Exception ignored) {
        }
        return body;
    }

    private <T> List<List<T>> partition(List<T> list, int batchSize) {
        if (list == null || list.isEmpty()) {
            return List.of();
        }
        int size = Math.max(1, batchSize);
        List<List<T>> result = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            result.add(new ArrayList<>(list.subList(i, Math.min(list.size(), i + size))));
        }
        return result;
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
