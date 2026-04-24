package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.response.EmbeddingProfileView;
import com.alikeyou.itmoduleai.dto.response.KnowledgeEmbeddingStatusResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeChunkEmbedding;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.provider.embedding.EmbeddingProfileInfo;
import com.alikeyou.itmoduleai.provider.embedding.EmbeddingProfileResolver;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@Slf4j
@RequiredArgsConstructor
public class KnowledgeEmbeddingServiceImpl implements KnowledgeEmbeddingService {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final ObjectMapper objectMapper;
    private final PlatformTransactionManager transactionManager;
    private final EmbeddingProviderManager embeddingProviderManager;
    private final EmbeddingProfileResolver embeddingProfileResolver;

    @Value("${ai.embedding.default-dimension:768}")
    private int defaultDimension;

    @Value("${ai.embedding.batch-size:25}")
    private int defaultBatchSize;

    private final ConcurrentMap<String, ReentrantLock> runningLocks = new ConcurrentHashMap<>();

    @Override
    public KnowledgeEmbeddingStatusResponse backfillDocumentEmbeddings(Long documentId, String provider, String modelName, Integer dimension) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge document not found"));
        KnowledgeBase knowledgeBase = loadKnowledgeBaseForDocument(document);
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
        EmbeddingProfileInfo profile = resolveProfile(knowledgeBase, "REQUEST", provider, modelName, dimension);
        String lockKey = buildLockKey("DOCUMENT", documentId, profile);
        ReentrantLock lock = runningLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResponseStatusException(CONFLICT, "Embedding backfill is already running for this document");
        }
        try {
            long createdCount = 0L;
            if (!chunks.isEmpty()) {
                for (List<KnowledgeChunk> batch : partition(chunks, profile.getBatchSize())) {
                    createdCount += writeEmbeddingBatch(batch, profile.getProvider(), profile.getModelName(), profile.getDimension());
                }
            }
            long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByDocumentId(documentId);
            return buildStatusResponse("DOCUMENT", documentId, chunks.size(), embeddedCount, createdCount, profile, inspectDocumentProfileState(documentId));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public KnowledgeEmbeddingStatusResponse backfillKnowledgeBaseEmbeddings(Long knowledgeBaseId, String provider, String modelName, Integer dimension) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge base not found"));
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByKnowledgeBase_IdOrderByDocument_IdAscChunkIndexAsc(knowledgeBaseId);
        EmbeddingProfileInfo profile = resolveProfile(knowledgeBase, "REQUEST", provider, modelName, dimension);
        String lockKey = buildLockKey("KB", knowledgeBaseId, profile);
        ReentrantLock lock = runningLocks.computeIfAbsent(lockKey, k -> new ReentrantLock());
        if (!lock.tryLock()) {
            throw new ResponseStatusException(CONFLICT, "Embedding backfill is already running for this knowledge base");
        }
        try {
            long createdCount = 0L;
            if (!chunks.isEmpty()) {
                for (List<KnowledgeChunk> batch : partition(chunks, profile.getBatchSize())) {
                    createdCount += writeEmbeddingBatch(batch, profile.getProvider(), profile.getModelName(), profile.getDimension());
                }
            }
            long embeddedCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByKnowledgeBaseId(knowledgeBaseId);
            return buildStatusResponse("KNOWLEDGE_BASE", knowledgeBaseId, chunks.size(), embeddedCount, createdCount, profile, inspectKnowledgeBaseProfileState(knowledgeBaseId));
        } finally {
            lock.unlock();
        }
    }

    @Override
    public List<Double> embedText(String text, String provider, String modelName, Integer dimension) {
        if (!StringUtils.hasText(text)) {
            return Collections.emptyList();
        }
        EmbeddingProfileInfo profile = resolveProfile(null, "EMBED", provider, modelName, dimension);
        if (!Boolean.TRUE.equals(profile.getProviderSupported())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unsupported embedding provider: " + profile.getProvider());
        }
        return embeddingProviderManager.resolve(profile.getProvider())
                .embed(EmbeddingRequest.builder()
                        .text(text)
                        .providerCode(profile.getProvider())
                        .modelName(profile.getModelName())
                        .dimension(profile.getDimension())
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

    @Override
    public KnowledgeEmbeddingStatusResponse getKnowledgeBaseEmbeddingStatus(Long knowledgeBaseId) {
        KnowledgeBase kb = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge base not found"));
        long totalChunkCount = knowledgeChunkRepository.countByKnowledgeBase_Id(knowledgeBaseId);
        long embeddedChunkCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByKnowledgeBaseId(knowledgeBaseId);
        EmbeddingProfileInfo profile = resolveProfile(kb, "KNOWLEDGE_BASE", null, null, null);
        return buildStatusResponse("KNOWLEDGE_BASE", knowledgeBaseId, totalChunkCount, embeddedChunkCount, 0L, profile, inspectKnowledgeBaseProfileState(knowledgeBaseId));
    }

    @Override
    public KnowledgeEmbeddingStatusResponse getDocumentEmbeddingStatus(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge document not found"));
        long totalChunkCount = knowledgeChunkRepository.countByDocument_Id(documentId);
        long embeddedChunkCount = knowledgeChunkEmbeddingRepository.countDistinctChunkByDocumentId(documentId);
        EmbeddingProfileInfo profile = resolveProfile(loadKnowledgeBaseForDocument(document), "KNOWLEDGE_BASE", null, null, null);
        return buildStatusResponse("DOCUMENT", documentId, totalChunkCount, embeddedChunkCount, 0L, profile, inspectDocumentProfileState(documentId));
    }

    private KnowledgeBase loadKnowledgeBaseForDocument(KnowledgeDocument document) {
        Long knowledgeBaseId = document == null ? null : document.getKnowledgeBaseId();
        if (knowledgeBaseId == null) {
            throw new ResponseStatusException(NOT_FOUND, "Knowledge base not found");
        }
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Knowledge base not found"));
    }

    private KnowledgeEmbeddingStatusResponse buildStatusResponse(String targetType,
                                                                  Long targetId,
                                                                  long totalChunkCount,
                                                                  long embeddedChunkCount,
                                                                  long createdCount,
                                                                  EmbeddingProfileInfo profile,
                                                                  ProfileState activeState) {
        EmbeddingProfileView profileView = buildProfileView(profile, activeState, embeddedChunkCount);
        return KnowledgeEmbeddingStatusResponse.builder()
                .targetType(targetType)
                .targetId(targetId)
                .totalChunkCount(totalChunkCount)
                .embeddedChunkCount(embeddedChunkCount)
                .createdEmbeddingCount(createdCount)
                .provider(profileView == null ? null : profileView.getProvider())
                .modelName(profileView == null ? null : profileView.getModelName())
                .dimension(profileView == null ? null : profileView.getDimension())
                .profileSource(profileView == null ? null : profileView.getSource())
                .profileWarning(profileView == null ? null : profileView.getWarning())
                .needsRebuild(profileView == null ? null : profileView.getNeedsRebuild())
                .batchSize(profileView == null ? null : profileView.getBatchSize())
                .configuredProvider(profileView == null ? null : profileView.getConfiguredProvider())
                .configuredModelName(profileView == null ? null : profileView.getConfiguredModelName())
                .activeProvider(profileView == null ? null : profileView.getActiveProvider())
                .activeModelName(profileView == null ? null : profileView.getActiveModelName())
                .activeDimension(profileView == null ? null : profileView.getActiveDimension())
                .activeEmbeddingCount(profileView == null ? null : profileView.getActiveEmbeddingCount())
                .embeddingProfile(profileView)
                .build();
    }

    private EmbeddingProfileView buildProfileView(EmbeddingProfileInfo profile, ProfileState activeState, long embeddedChunkCount) {
        if (profile == null && activeState == null) {
            return null;
        }
        boolean needsRebuild = embeddedChunkCount > 0
                && (activeState == null || !activeState.hasStableProfile() || !matches(profile, activeState));
        String warning = joinWarnings(profile == null ? null : profile.getWarning(), activeState == null ? null : activeState.warning());
        return EmbeddingProfileView.builder()
                .requestedProvider(profile == null ? null : profile.getRequestedProvider())
                .requestedModelName(profile == null ? null : profile.getRequestedModelName())
                .requestedDimension(profile == null ? null : profile.getRequestedDimension())
                .configuredProvider(profile == null ? null : profile.getConfiguredProvider())
                .configuredModelName(profile == null ? null : profile.getConfiguredModelName())
                .provider(profile == null ? null : profile.getProvider())
                .modelName(profile == null ? null : profile.getModelName())
                .dimension(profile == null ? null : profile.getDimension())
                .batchSize(profile == null ? Math.max(1, defaultBatchSize) : profile.getBatchSize())
                .source(profile == null ? null : profile.getSource())
                .providerSupported(profile == null ? null : profile.getProviderSupported())
                .warning(warning)
                .activeProvider(activeState == null ? null : activeState.provider())
                .activeModelName(activeState == null ? null : activeState.modelName())
                .activeDimension(activeState == null ? null : activeState.dimension())
                .activeEmbeddingCount(activeState == null ? null : activeState.activeCount())
                .needsRebuild(needsRebuild)
                .build();
    }

    private boolean matches(EmbeddingProfileInfo profile, ProfileState activeState) {
        if (profile == null || activeState == null || !activeState.hasStableProfile()) {
            return false;
        }
        return equalsText(profile.getProvider(), activeState.provider())
                && equalsText(profile.getModelName(), activeState.modelName())
                && equalsInteger(profile.getDimension(), activeState.dimension());
    }

    private boolean equalsText(String left, String right) {
        return normalizeForCompare(left).equals(normalizeForCompare(right));
    }

    private String normalizeForCompare(String value) {
        String text = trimToNull(value);
        return text == null ? "" : text;
    }

    private boolean equalsInteger(Integer left, Integer right) {
        return left == null ? right == null : left.equals(right);
    }

    private String joinWarnings(String... values) {
        List<String> parts = new ArrayList<>();
        if (values != null) {
            for (String value : values) {
                String text = trimToNull(value);
                if (StringUtils.hasText(text)) {
                    parts.add(text);
                }
            }
        }
        return parts.isEmpty() ? null : String.join("; ", parts);
    }

    private EmbeddingProfileInfo resolveProfile(KnowledgeBase knowledgeBase,
                                                String source,
                                                String provider,
                                                String modelName,
                                                Integer dimension) {
        return embeddingProfileResolver.resolve(knowledgeBase, source, provider, modelName, dimension);
    }

    private ProfileState inspectKnowledgeBaseProfileState(Long knowledgeBaseId) {
        List<KnowledgeChunkEmbedding> embeddings = knowledgeChunkEmbeddingRepository.findLatestByKnowledgeBaseIds(List.of(knowledgeBaseId));
        return inspectProfileState(embeddings, "knowledge base " + knowledgeBaseId);
    }

    private ProfileState inspectDocumentProfileState(Long documentId) {
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
        if (chunks == null || chunks.isEmpty()) {
            return ProfileState.empty();
        }
        List<Long> chunkIds = chunks.stream()
                .map(KnowledgeChunk::getId)
                .filter(id -> id != null)
                .toList();
        if (chunkIds.isEmpty()) {
            return ProfileState.empty();
        }
        List<KnowledgeChunkEmbedding> embeddings = knowledgeChunkEmbeddingRepository.findLatestByChunkIds(chunkIds);
        return inspectProfileState(embeddings, "document " + documentId);
    }

    private ProfileState inspectProfileState(List<KnowledgeChunkEmbedding> embeddings, String scopeLabel) {
        if (embeddings == null || embeddings.isEmpty()) {
            return ProfileState.empty();
        }
        String provider = null;
        String modelName = null;
        Integer dimension = null;
        long activeCount = 0L;
        boolean mixed = false;
        for (KnowledgeChunkEmbedding embedding : embeddings) {
            if (embedding == null || embedding.getStatus() != KnowledgeChunkEmbedding.Status.ACTIVE) {
                continue;
            }
            activeCount++;
            String currentProvider = normalizeProvider(embedding.getProviderCode());
            String currentModel = normalizeModel(embedding.getModelName());
            Integer currentDimension = embedding.getDimension();
            if (provider == null) {
                provider = currentProvider;
            } else if (!equalsText(provider, currentProvider)) {
                mixed = true;
            }
            if (modelName == null) {
                modelName = currentModel;
            } else if (!equalsText(modelName, currentModel)) {
                mixed = true;
            }
            if (dimension == null) {
                dimension = currentDimension;
            } else if (!equalsInteger(dimension, currentDimension)) {
                mixed = true;
            }
        }
        if (activeCount == 0L) {
            return ProfileState.empty();
        }
        String warning = mixed ? "active embeddings have mixed provider/model/dimension values in " + scopeLabel : null;
        return new ProfileState(provider, modelName, dimension, activeCount, warning);
    }

    private long writeEmbeddingBatch(List<KnowledgeChunk> batch, String provider, String modelName, Integer fallbackDimension) {
        if (batch == null || batch.isEmpty()) {
            return 0L;
        }
        Map<Long, KnowledgeDocument> documentsById = loadDocumentsForChunks(batch);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        tx.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        Long value = tx.execute(status -> {
            long count = 0L;
            for (KnowledgeChunk chunk : batch) {
                if (chunk == null || !StringUtils.hasText(chunk.getContent())) {
                    continue;
                }
                try {
                    List<Double> vector = embedText(buildEmbeddingInput(chunk, documentsById.get(chunk.getDocumentId())), provider, modelName, fallbackDimension);
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
                    log.warn("Embedding failed for chunkId={} provider={} model={} reason={}",
                            chunk.getId(), provider, modelName, ex.getMessage());
                }
            }
            return count;
        });
        return value == null ? 0L : value;
    }

    private Map<Long, KnowledgeDocument> loadDocumentsForChunks(List<KnowledgeChunk> chunks) {
        Map<Long, KnowledgeDocument> documentsById = new LinkedHashMap<>();
        if (chunks == null || chunks.isEmpty()) {
            return documentsById;
        }
        List<Long> documentIds = chunks.stream()
                .map(KnowledgeChunk::getDocumentId)
                .filter(id -> id != null && !documentsById.containsKey(id))
                .distinct()
                .toList();
        if (documentIds.isEmpty()) {
            return documentsById;
        }
        knowledgeDocumentRepository.findAllById(documentIds)
                .forEach(document -> {
                    if (document != null && document.getId() != null) {
                        documentsById.put(document.getId(), document);
                    }
                });
        return documentsById;
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

    private String buildEmbeddingInput(KnowledgeChunk chunk, KnowledgeDocument document) {
        StringBuilder sb = new StringBuilder();
        String fileName = document == null ? null : trimToNull(document.getFileName());
        String title = document == null ? null : trimToNull(document.getTitle());
        String archiveEntryPath = document == null ? null : trimToNull(document.getArchiveEntryPath());
        String sourceUrl = document == null ? null : trimToNull(document.getSourceUrl());
        String chunkFilePath = trimToNull(chunk == null ? null : chunk.getFilePath());
        String path = StringUtils.hasText(chunkFilePath)
                ? chunkFilePath
                : StringUtils.hasText(archiveEntryPath)
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

    private String normalizeProvider(String provider) {
        return EmbeddingNameNormalizer.normalizeProvider(provider);
    }

    private String normalizeModel(String modelName) {
        return EmbeddingNameNormalizer.normalizeModel(modelName);
    }

    private Integer normalizeDimension(Integer dimension) {
        if (dimension == null) {
            return Math.max(32, Math.min(defaultDimension, 4096));
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

    private String buildLockKey(String prefix, Long id, EmbeddingProfileInfo profile) {
        return prefix + ":" + id + ":" + profile.getProvider() + ":" + profile.getModelName() + ":" + profile.getDimension();
    }

    private String trimToNull(String value) {
        return EmbeddingNameNormalizer.trimToNull(value);
    }

    private record ProfileState(String provider, String modelName, Integer dimension, long activeCount, String warning) {
        static ProfileState empty() {
            return new ProfileState(null, null, null, 0L, null);
        }

        boolean hasStableProfile() {
            return StringUtils.hasText(provider) && StringUtils.hasText(modelName) && dimension != null;
        }
    }
}
