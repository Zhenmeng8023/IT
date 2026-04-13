package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.provider.embedding.EmbeddingProviderManager;
import com.alikeyou.itmoduleai.provider.embedding.EmbeddingRequest;
import com.alikeyou.itmoduleai.provider.support.EmbeddingNameNormalizer;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

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
    private final EmbeddingProviderManager embeddingProviderManager;

    @Value("${ai.embedding.default-provider:ollama}")
    private String defaultProvider;

    @Value("${ai.embedding.default-model:embeddinggemma:300m}")
    private String defaultModel;

    private final ConcurrentMap<String, ReentrantLock> runningLocks = new ConcurrentHashMap<>();

    @Override
    public KnowledgeEmbeddingStatusResponse backfillDocumentEmbeddings(Long documentId, String provider, String modelName, Integer dimension) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge document not found"));
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
            throw new ResponseStatusException(CONFLICT, "Embedding backfill is already running for this document");
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
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge base not found"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId);
        if (chunks.isEmpty()) {
            return buildKnowledgeBaseStatus(knowledgeBaseId, knowledgeBase, provider, modelName, dimension, 0L, 0L);
        }
        String actualProvider = resolveProvider(provider, knowledgeBase);
        String actualModel = resolveModel(modelName, knowledgeBase);
        String lockKey = "KB:" + knowledgeBaseId + ":" + actualProvider + ":" + actualModel;
        ReentrantLock lock = runningLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResponseStatusException(CONFLICT, "Embedding backfill is already running for this knowledge base");
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
        if (!StringUtils.hasText(actualProvider)) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing embedding provider");
        }
        if (!StringUtils.hasText(actualModel)) {
            throw new ResponseStatusException(BAD_REQUEST, "Missing embedding model");
        }
        return embeddingProviderManager.resolve(actualProvider)
                .embed(EmbeddingRequest.builder()
                        .text(text)
                        .providerCode(actualProvider)
                        .modelName(actualModel)
                        .dimension(normalizeDimension(dimension))
                        .build());
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
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge base not found"));
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
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge document not found"));
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
                try {
                    List<Double> vector = embedText(buildEmbeddingInput(chunk), provider, modelName, fallbackDimension);
                    KnowledgeChunkEmbedding saved = saveEmbeddingVersion(
                            chunk,
                            provider,
                            modelName,
                            vector.isEmpty() ? normalizeDimension(fallbackDimension) : vector.size(),
                            vector,
                            KnowledgeChunkEmbedding.Status.ACTIVE
                    );

                    chunk.setEmbeddingProvider(provider);
                    chunk.setEmbeddingModel(modelName);
                    chunk.setVectorId(StringUtils.hasText(saved.getVectorRef()) ? saved.getVectorRef() : String.valueOf(saved.getId()));
                    knowledgeChunkRepository.save(chunk);
                    count++;
                } catch (RuntimeException ex) {
                    saveEmbeddingVersion(
                            chunk,
                            provider,
                            modelName,
                            normalizeDimension(fallbackDimension),
                            List.of(),
                            KnowledgeChunkEmbedding.Status.FAILED
                    );
                }
            }
            return count;
        });
        return value == null ? 0L : value;
    }

    private KnowledgeChunkEmbedding saveEmbeddingVersion(KnowledgeChunk chunk,
                                                         String provider,
                                                         String modelName,
                                                         Integer dimension,
                                                         List<Double> vector,
                                                         KnowledgeChunkEmbedding.Status status) {
        KnowledgeChunkEmbedding embedding = knowledgeChunkEmbeddingRepository
                .findTopByChunk_IdAndProviderCodeAndModelNameOrderByIdDesc(chunk.getId(), provider, modelName)
                .orElseGet(KnowledgeChunkEmbedding::new);
        embedding.setChunk(chunk);
        embedding.setProviderCode(provider);
        embedding.setModelName(modelName);
        embedding.setDimension(dimension);
        embedding.setVectorPayload(toJson(vector));
        embedding.setVectorRef(buildVectorRef(chunk.getId(), provider, modelName));
        embedding.setStatus(status);
        return knowledgeChunkEmbeddingRepository.save(embedding);
    }

    private String buildEmbeddingInput(KnowledgeChunk chunk) {
        StringBuilder sb = new StringBuilder();
        KnowledgeDocument document = chunk.getDocument();
        String fileName = document == null ? null : trimToNull(document.getFileName());
        String title = document == null ? null : trimToNull(document.getTitle());
        String archiveEntryPath = document == null ? null : trimToNull(document.getArchiveEntryPath());
        String sourceUrl = document == null ? null : trimToNull(document.getSourceUrl());
        String path = StringUtils.hasText(archiveEntryPath)
                ? archiveEntryPath
                : (StringUtils.hasText(sourceUrl) ? sourceUrl : fileName);

        if (StringUtils.hasText(path)) {
            sb.append("path: ").append(path).append('\n');
        }
        if (StringUtils.hasText(fileName)) {
            sb.append("file: ").append(fileName).append('\n');
        }
        if (StringUtils.hasText(title)) {
            sb.append("title: ").append(title).append('\n');
        }
        if (StringUtils.hasText(chunk.getMetadataJson())) {
            sb.append("metadata: ").append(chunk.getMetadataJson()).append('\n');
        }
        sb.append("chunk: ").append(chunk.getChunkIndex() == null ? 0 : chunk.getChunkIndex()).append('\n');
        sb.append("content:\n").append(chunk.getContent().trim());
        return sb.toString();
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
            throw new ResponseStatusException(BAD_REQUEST, "Missing embedding provider");
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
            throw new ResponseStatusException(BAD_REQUEST, "Missing embedding model");
        }
        return normalizeModel(resolved);
    }

    private String normalizeProvider(String provider) {
        return EmbeddingNameNormalizer.normalizeProvider(provider);
    }

    private String normalizeModel(String modelName) {
        return EmbeddingNameNormalizer.normalizeModel(modelName);
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
        return EmbeddingNameNormalizer.trimToNull(value);
    }
}
