package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class KnowledgeEmbeddingServiceImpl implements KnowledgeEmbeddingService {

    private static final int DEFAULT_DIMENSION = 128;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public KnowledgeEmbeddingStatusResponse backfillDocumentEmbeddings(Long documentId, String provider, String modelName, Integer dimension) {
        knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识文档不存在"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
        long createdCount = writeEmbeddings(chunks, provider, modelName, dimension);
        long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByDocumentId(documentId);
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("DOCUMENT")
                .targetId(documentId)
                .totalChunkCount((long) chunks.size())
                .embeddedChunkCount(embeddedCount)
                .createdEmbeddingCount(createdCount)
                .provider(resolveProvider(provider, chunks.isEmpty() ? null : chunks.get(0).getKnowledgeBase()))
                .modelName(resolveModel(modelName, chunks.isEmpty() ? null : chunks.get(0).getKnowledgeBase(), normalizeDimension(dimension)))
                .dimension(normalizeDimension(dimension))
                .build();
    }

    @Override
    @Transactional
    public KnowledgeEmbeddingStatusResponse backfillKnowledgeBaseEmbeddings(Long knowledgeBaseId, String provider, String modelName, Integer dimension) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "知识库不存在"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId);
        long createdCount = writeEmbeddings(chunks, provider, modelName, dimension);
        long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByKnowledgeBaseId(knowledgeBaseId);
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType("KNOWLEDGE_BASE")
                .targetId(knowledgeBaseId)
                .totalChunkCount((long) chunks.size())
                .embeddedChunkCount(embeddedCount)
                .createdEmbeddingCount(createdCount)
                .provider(resolveProvider(provider, knowledgeBase))
                .modelName(resolveModel(modelName, knowledgeBase, normalizeDimension(dimension)))
                .dimension(normalizeDimension(dimension))
                .build();
    }

    @Override
    public List<Double> embedText(String text, String provider, String modelName, Integer dimension) {
        String normalized = normalizeText(text);
        if (!StringUtils.hasText(normalized)) {
            return Collections.nCopies(normalizeDimension(dimension), 0D);
        }
        int dim = normalizeDimension(dimension);
        double[] vector = new double[dim];
        for (String token : normalized.split("\\s+")) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            int hash = token.hashCode();
            int bucket = Math.floorMod(hash, dim);
            double weight = 1D + Math.min(token.length(), 12) * 0.03D;
            vector[bucket] += weight;
        }
        normalizeVector(vector);
        List<Double> result = new ArrayList<>(dim);
        for (double value : vector) {
            result.add(value);
        }
        return result;
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

    private long writeEmbeddings(List<KnowledgeChunk> chunks, String provider, String modelName, Integer dimension) {
        if (chunks == null || chunks.isEmpty()) {
            return 0L;
        }
        long count = 0L;
        int dim = normalizeDimension(dimension);
        for (KnowledgeChunk chunk : chunks) {
            if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
                continue;
            }
            KnowledgeBase knowledgeBase = chunk.getKnowledgeBase();
            String actualProvider = resolveProvider(provider, knowledgeBase);
            String actualModel = resolveModel(modelName, knowledgeBase, dim);
            List<Double> vector = embedText(chunk.getContent(), actualProvider, actualModel, dim);

            KnowledgeChunkEmbedding embedding = knowledgeChunkEmbeddingRepository
                    .findFirstByChunk_IdAndProviderCodeAndModelName(chunk.getId(), actualProvider, actualModel)
                    .orElseGet(() -> {
                        KnowledgeChunkEmbedding item = new KnowledgeChunkEmbedding();
                        item.setChunk(chunk);
                        item.setProviderCode(actualProvider);
                        item.setModelName(actualModel);
                        return item;
                    });

            embedding.setDimension(dim);
            embedding.setVectorPayload(toJson(vector));
            embedding.setVectorRef(buildVectorRef(chunk.getId(), actualProvider, actualModel));
            embedding.setStatus(KnowledgeChunkEmbedding.Status.ACTIVE);
            KnowledgeChunkEmbedding saved = knowledgeChunkEmbeddingRepository.save(embedding);

            chunk.setEmbeddingProvider(actualProvider);
            chunk.setEmbeddingModel(actualModel);
            chunk.setVectorId(resolveChunkVectorId(saved));
            knowledgeChunkRepository.save(chunk);
            count++;
        }
        return count;
    }

    private String resolveProvider(String provider, KnowledgeBase knowledgeBase) {
        if (StringUtils.hasText(provider) && !"NONE".equalsIgnoreCase(provider.trim())) {
            return provider.trim();
        }
        if (knowledgeBase != null
                && StringUtils.hasText(knowledgeBase.getEmbeddingProvider())
                && !"NONE".equalsIgnoreCase(knowledgeBase.getEmbeddingProvider().trim())) {
            return knowledgeBase.getEmbeddingProvider().trim();
        }
        return "local-hash";
    }

    private String resolveModel(String modelName, KnowledgeBase knowledgeBase, Integer dimension) {
        if (StringUtils.hasText(modelName) && !"NONE".equalsIgnoreCase(modelName.trim())) {
            return modelName.trim();
        }
        if (knowledgeBase != null
                && StringUtils.hasText(knowledgeBase.getEmbeddingModel())
                && !"NONE".equalsIgnoreCase(knowledgeBase.getEmbeddingModel().trim())) {
            return knowledgeBase.getEmbeddingModel().trim();
        }
        return "local-hash-" + dimension;
    }

    private Integer normalizeDimension(Integer dimension) {
        if (dimension == null) {
            return DEFAULT_DIMENSION;
        }
        return Math.max(32, Math.min(dimension, 2048));
    }

    private String buildVectorRef(Long chunkId, String provider, String modelName) {
        return provider + ":" + modelName + ":" + chunkId + ":" + System.currentTimeMillis();
    }

    private String resolveChunkVectorId(KnowledgeChunkEmbedding embedding) {
        if (embedding == null) {
            return null;
        }
        if (StringUtils.hasText(embedding.getVectorRef())) {
            return embedding.getVectorRef();
        }
        return embedding.getId() == null ? null : String.valueOf(embedding.getId());
    }

    private String toJson(List<Double> vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    private String normalizeText(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        return text.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void normalizeVector(double[] vector) {
        double norm = 0D;
        for (double value : vector) {
            norm += value * value;
        }
        norm = Math.sqrt(norm);
        if (norm <= 0D) {
            return;
        }
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }
}
