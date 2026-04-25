package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.config.AiKnowledgeTaskExecutorConfig;
import com.alikeyou.itmoduleai.dto.common.KnowledgeDocumentBinary;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeBaseMemberCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.dto.request.KnowledgeIndexTaskCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.provider.support.EmbeddingNameNormalizer;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.policy.KnowledgeBaseScopePolicy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int ERROR_MESSAGE_MAX_LENGTH = 500;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final CodeIndexService codeIndexService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final KnowledgeAccessGuard knowledgeAccessGuard;
    private final KnowledgeBaseScopePolicy knowledgeBaseScopePolicy;
    private final AiCurrentUserProvider currentUserProvider;
    @Qualifier(AiKnowledgeTaskExecutorConfig.AI_KNOWLEDGE_TASK_EXECUTOR)
    private final Executor aiKnowledgeTaskExecutor;

    @Value("${app.ai.knowledge-document.storage-root:${user.dir}/.runtime/knowledge-documents}")
    private String storageRoot;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        validateKnowledgeBaseRequest(request);
        KnowledgeBase.ScopeType scopeType = knowledgeBaseScopePolicy.normalizeScope(request.getScopeType());

        Instant now = Instant.now();
        KnowledgeBase entity = new KnowledgeBase();
        entity.setScopeType(scopeType);
        entity.setProjectId(request.getProjectId());
        entity.setOwnerId(request.getOwnerId());
        entity.setName(trimToNull(request.getName()));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setSourceType(request.getSourceType() == null ? KnowledgeBase.SourceType.MANUAL : request.getSourceType());
        entity.setChunkStrategy(request.getChunkStrategy() == null ? KnowledgeBase.ChunkStrategy.PARAGRAPH : request.getChunkStrategy());
        entity.setDefaultTopK(request.getDefaultTopK() == null ? DEFAULT_TOP_K : request.getDefaultTopK());
        entity.setVisibility(request.getVisibility() == null ? KnowledgeBase.Visibility.PRIVATE : request.getVisibility());
        applyEmbeddingConfig(entity, request.getEmbeddingProvider(), request.getEmbeddingModel());
        entity.setStatus(KnowledgeBase.Status.DRAFT);
        entity.setDocCount(0);
        entity.setChunkCount(0);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        KnowledgeBase saved = knowledgeBaseRepository.save(entity);
        ensureOwnerMember(saved);
        return saved;
    }

    @Override
    public KnowledgeBase updateKnowledgeBase(Long id, KnowledgeBaseCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Knowledge base request cannot be null");
        }

        KnowledgeBase entity = knowledgeAccessGuard.requireKnowledgeBaseEdit(id);
        boolean canUpdateOwnerFields = knowledgeAccessGuard.hasKnowledgeBaseOwnerAccess(id);
        KnowledgeBase.ScopeType nextScopeType = canUpdateOwnerFields && request.getScopeType() != null
                ? request.getScopeType()
                : entity.getScopeType();
        Long nextProjectId = canUpdateOwnerFields && request.getProjectId() != null
                ? request.getProjectId()
                : entity.getProjectId();
        Long nextOwnerId = canUpdateOwnerFields && request.getOwnerId() != null
                ? request.getOwnerId()
                : entity.getOwnerId();
        knowledgeBaseScopePolicy.validateScopeBinding(nextScopeType, nextOwnerId, nextProjectId);

        if (canUpdateOwnerFields && request.getScopeType() != null) entity.setScopeType(request.getScopeType());
        if (canUpdateOwnerFields && request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (canUpdateOwnerFields && request.getOwnerId() != null) entity.setOwnerId(request.getOwnerId());
        if (StringUtils.hasText(request.getName())) entity.setName(trimToNull(request.getName()));
        if (request.getDescription() != null) entity.setDescription(trimToNull(request.getDescription()));
        if (request.getSourceType() != null) entity.setSourceType(request.getSourceType());
        if (request.getChunkStrategy() != null) entity.setChunkStrategy(request.getChunkStrategy());
        if (request.getDefaultTopK() != null) entity.setDefaultTopK(request.getDefaultTopK());
        if (request.getVisibility() != null) entity.setVisibility(request.getVisibility());
        applyEmbeddingConfig(entity, request.getEmbeddingProvider(), request.getEmbeddingModel());
        entity.setUpdatedAt(Instant.now());

        KnowledgeBase saved = knowledgeBaseRepository.save(entity);
        ensureOwnerMember(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getById(Long id) {
        return knowledgeAccessGuard.requireKnowledgeBaseRead(id);
    }

    @Override
    public void deleteKnowledgeBase(Long id) {
        knowledgeAccessGuard.requireKnowledgeBaseOwner(id);
        List<String> storagePaths = knowledgeBaseRepository.findDocumentStoragePaths(id);
        int affected = knowledgeBaseRepository.deleteByIdInBulk(id);
        if (affected <= 0) {
            throw new RuntimeException("Knowledge base not found");
        }

        storagePaths.forEach(this::deleteStoredDocumentFile);
    }

    @Override
    public KnowledgeBase updateKnowledgeBaseStatus(Long id, KnowledgeBase.Status status) {
        if (status == null) {
            throw new IllegalArgumentException("Knowledge base status cannot be null");
        }
        KnowledgeBase knowledgeBase = findKnowledgeBaseRequired(id);
        knowledgeBase.setStatus(status);
        knowledgeBase.setUpdatedAt(Instant.now());
        return knowledgeBaseRepository.save(knowledgeBase);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByOwner(Long ownerId, Pageable pageable) {
        if (currentUserProvider.isAdminAiViewer()) {
            return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        Long effectiveOwnerId = ownerId == null ? currentUserId : ownerId;
        if (!Objects.equals(effectiveOwnerId, currentUserId)) {
            effectiveOwnerId = currentUserId;
        }
        return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(effectiveOwnerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByProject(Long projectId, Pageable pageable) {
        if (currentUserProvider.isAdminAiViewer()) {
            return knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        return knowledgeBaseRepository.findAccessibleProjectPage(projectId, currentUserId, pageable);
    }

    @Override
    public KnowledgeDocument addDocument(Long knowledgeBaseId, KnowledgeDocumentCreateRequest request) {
        validateDocumentRequest(request);

        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        Instant now = Instant.now();

        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(request.getSourceType() == null ? KnowledgeDocument.SourceType.MANUAL : request.getSourceType());
        entity.setSourceRefId(request.getSourceRefId());
        entity.setSourceUrl(trimToNull(request.getSourceUrl()));
        entity.setTitle(resolveDocumentTitle(request.getTitle(), request.getFileName(), request.getContentText()));
        entity.setFileName(trimToNull(request.getFileName()));
        entity.setMimeType(trimToNull(request.getMimeType()));
        entity.setContentText(normalizeContent(request.getContentText()));
        entity.setContentHash(StringUtils.hasText(request.getContentHash()) ? trimToNull(request.getContentHash()) : sha256(entity.getContentText()));
        entity.setLanguage(trimToNull(request.getLanguage()));
        entity.setUploadedBy(request.getUploadedBy());
        entity.setStatus(KnowledgeDocument.Status.UPLOADED);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        return persistDocumentAndMaybeIndex(knowledgeBase, entity, shouldAutoIndex(request));
    }

    @Override
    public List<KnowledgeDocument> uploadDocuments(Long knowledgeBaseId, List<MultipartFile> files, KnowledgeDocumentCreateRequest request) {
        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("files娑撳秷鍏樻稉铏光敄");
        }

        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        List<KnowledgeDocument> results = new ArrayList<>();
        boolean singleFile = files.size() == 1;

        for (MultipartFile file : files) {
            if (file == null || file.isEmpty()) {
                continue;
            }
            results.add(uploadSingleDocument(knowledgeBase, file, request, singleFile));
        }

        refreshKnowledgeBaseStats(knowledgeBaseId, null);
        return results;
    }

    @Override
    public List<KnowledgeDocument> uploadZipDocuments(Long knowledgeBaseId, MultipartFile file, KnowledgeDocumentCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP file must not be empty");
        }

        String zipName = sanitizeFileName(file.getOriginalFilename());
        if (!zipName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("Only .zip files are supported");
        }

        Path tempDir = null;
        List<KnowledgeDocument> results = new ArrayList<>();
        long totalBytes = 0L;
        int fileCount = 0;
        final long maxTotalBytes = 200L * 1024 * 1024;
        final long maxEntryBytes = 10L * 1024 * 1024;
        final int maxFileCount = 2000;

        try {
            tempDir = Files.createTempDirectory("kb-zip-" + knowledgeBaseId + "-");

            try (ZipInputStream zis = new ZipInputStream(new BufferedInputStream(file.getInputStream()), StandardCharsets.UTF_8)) {
                ZipEntry entry;
                while ((entry = zis.getNextEntry()) != null) {
                    if (entry.isDirectory()) {
                        continue;
                    }

                    String entryName = normalizeZipEntryName(entry.getName());
                    if (!StringUtils.hasText(entryName) || shouldSkipProjectEntry(entryName)) {
                        continue;
                    }

                    Path extractedPath = tempDir.resolve(entryName).normalize();
                    if (!extractedPath.startsWith(tempDir)) {
                        throw new RuntimeException("闂堢偞纭?ZIP 鐠侯垰绶? " + entryName);
                    }

                    if (extractedPath.getParent() != null) {
                        Files.createDirectories(extractedPath.getParent());
                    }

                    long written = copyZipEntry(zis, extractedPath, maxEntryBytes);
                    totalBytes += written;
                    fileCount++;

                    if (totalBytes > maxTotalBytes) {
                        throw new RuntimeException("ZIP extracted size exceeds limit");
                    }
                    if (fileCount > maxFileCount) {
                        throw new RuntimeException("ZIP file count exceeds limit");
                    }

                    String ext = getExtension(extractedPath.getFileName().toString());
                    if (!isImportableExtension(ext)) {
                        continue;
                    }

                    results.add(importExtractedProjectFile(knowledgeBase, extractedPath, entryName, zipName, request));
                }
            }

            refreshKnowledgeBaseStats(knowledgeBaseId, null);
            return results;
        } catch (IOException ex) {
            throw new RuntimeException("ZIP 鐎电厧鍙嗘径杈Е: " + trimError(ex.getMessage()), ex);
        } finally {
            if (tempDir != null) {
                try {
                    FileSystemUtils.deleteRecursively(tempDir);
                } catch (IOException ignored) {
                }
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeDocument> pageDocuments(Long knowledgeBaseId, Pageable pageable) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByUpdatedAtDesc(knowledgeBaseId, pageable);
    }

    @Override
    public void deleteDocument(Long knowledgeBaseId, Long documentId) {
        knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeDocument document = loadDocument(knowledgeBaseId, documentId);
        String storagePath = document.getStoragePath();
        knowledgeDocumentRepository.delete(document);
        knowledgeDocumentRepository.flush();
        deleteStoredDocumentFile(storagePath);
        refreshKnowledgeBaseStats(knowledgeBaseId, null);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> listChunks(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);
        return knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentBinary downloadDocument(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);

        try {
            if (document.hasStoredFile()) {
                Path storedPath = resolveStoredPath(document.getStoragePath());
                return new KnowledgeDocumentBinary(
                        ensureDownloadFileName(document),
                        resolveMimeType(storedPath, document.getFileName(), MediaType.APPLICATION_OCTET_STREAM_VALUE),
                        Files.readAllBytes(storedPath)
                );
            }

            String fallbackName = ensureTextFileName(resolveDocumentTitle(document.getTitle(), document.getFileName(), document.getContentText()));
            String content = normalizeContent(document.getContentText());
            return new KnowledgeDocumentBinary(fallbackName, "text/plain;charset=UTF-8", content.getBytes(StandardCharsets.UTF_8));
        } catch (IOException ex) {
            throw new RuntimeException("娑撳娴囬弬鍥︽婢惰精瑙? " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentBinary downloadDocumentsZip(Long knowledgeBaseId, List<Long> documentIds) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        List<KnowledgeDocument> documents = loadDownloadDocuments(knowledgeBaseId, documentIds);
        if (documents.isEmpty()) {
            throw new RuntimeException("No downloadable documents found");
        }

        try (ByteArrayOutputStream bos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(bos, StandardCharsets.UTF_8)) {
            Set<String> usedNames = new HashSet<>();

            for (KnowledgeDocument document : documents) {
                byte[] content;
                String entryName;

                if (document.hasStoredFile()) {
                    Path storedPath = resolveStoredPath(document.getStoragePath());
                    content = Files.readAllBytes(storedPath);
                    entryName = ensureUniqueFileName(usedNames, ensureDownloadFileName(document));
                } else {
                    String text = normalizeContent(document.getContentText());
                    content = text.getBytes(StandardCharsets.UTF_8);
                    entryName = ensureUniqueFileName(usedNames,
                            ensureTextFileName(resolveDocumentTitle(document.getTitle(), document.getFileName(), text)));
                }

                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(content);
                zos.closeEntry();
            }
            zos.finish();
            return new KnowledgeDocumentBinary(
                    sanitizeFileName(knowledgeBase.getName()) + "-documents.zip",
                    "application/zip",
                    bos.toByteArray()
            );
        } catch (IOException ex) {
            throw new RuntimeException("閹垫挸瀵樻稉瀣祰婢惰精瑙? " + ex.getMessage(), ex);
        }
    }

    @Override
    public KnowledgeBaseMember addMember(Long knowledgeBaseId, KnowledgeBaseMemberCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId娑撳秷鍏樻稉铏光敄");
        }

        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseOwner(knowledgeBaseId);
        if (knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(knowledgeBaseId, request.getUserId())) {
            throw new RuntimeException("Member already exists");
        }

        KnowledgeBaseMember entity = new KnowledgeBaseMember();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setUserId(request.getUserId());
        entity.setRoleCode(request.getRoleCode() == null ? KnowledgeBaseMember.RoleCode.VIEWER : request.getRoleCode());
        entity.setGrantedBy(knowledgeBase.getOwnerId());
        entity.setCreatedAt(Instant.now());
        return knowledgeBaseMemberRepository.save(entity);
    }

    @Override
    public void removeMember(Long knowledgeBaseId, Long memberId) {
        knowledgeAccessGuard.requireKnowledgeBaseOwner(knowledgeBaseId);
        KnowledgeBaseMember entity = knowledgeBaseMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("Knowledge base member not found"));
        if (!entity.getKnowledgeBase().getId().equals(knowledgeBaseId)) {
            throw new RuntimeException("Member does not belong to this knowledge base");
        }
        knowledgeBaseMemberRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseMember> listMembers(Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return knowledgeBaseMemberRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);
    }

    @Override
    public KnowledgeIndexTask createIndexTask(Long knowledgeBaseId, KnowledgeIndexTaskCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);
        KnowledgeDocument document = null;
        if (request != null && request.getDocumentId() != null) {
            document = loadDocument(knowledgeBaseId, request.getDocumentId());
        }
        KnowledgeIndexTask entity = createPendingIndexTask(
                knowledgeBase,
                document,
                resolveTaskType(request)
        );
        submitIndexTask(entity.getId());
        return entity;
    }

    private KnowledgeIndexTask createPendingIndexTask(KnowledgeBase knowledgeBase,
                                                      KnowledgeDocument document,
                                                      KnowledgeIndexTask.TaskType taskType) {
        Instant now = Instant.now();
        KnowledgeIndexTask entity = new KnowledgeIndexTask();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setDocument(document);
        entity.setTaskType(taskType == null ? KnowledgeIndexTask.TaskType.REINDEX : taskType);
        entity.setStatus(KnowledgeIndexTask.Status.PENDING);
        entity.setRetryCount(0);
        entity.setErrorMessage(null);
        entity.setStartedAt(null);
        entity.setFinishedAt(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return knowledgeIndexTaskRepository.save(entity);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumePendingIndexTasksOnStartup() {
        List<KnowledgeIndexTask> pendingTasks = knowledgeIndexTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeIndexTask.Status.PENDING);
        List<KnowledgeIndexTask> runningTasks = knowledgeIndexTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeIndexTask.Status.RUNNING);
        Instant now = Instant.now();
        runningTasks.forEach(task -> knowledgeIndexTaskRepository.resetStatus(
                task.getId(),
                KnowledgeIndexTask.Status.RUNNING,
                KnowledgeIndexTask.Status.PENDING,
                "Recovered stale RUNNING task after application restart",
                now
        ));
        if (pendingTasks.isEmpty() && runningTasks.isEmpty()) {
            return;
        }
        List<KnowledgeIndexTask> recoverableTasks = knowledgeIndexTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeIndexTask.Status.PENDING);
        log.info("Resubmitting {} recoverable knowledge index task(s) on startup", recoverableTasks.size());
        recoverableTasks.forEach(task -> submitIndexTask(task.getId()));
    }

    private void submitIndexTask(Long taskId) {
        if (taskId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSubmitIndexTask(taskId);
                }
            });
            return;
        }
        doSubmitIndexTask(taskId);
    }

    private void doSubmitIndexTask(Long taskId) {
        try {
            aiKnowledgeTaskExecutor.execute(() -> runIndexTask(taskId));
        } catch (RejectedExecutionException ex) {
            String error = trimError("Task queue is full: " + ex.getMessage());
            log.error("Failed to submit knowledge index task {}: {}", taskId, error, ex);
            failBeforeStart(taskId, error);
        }
    }

    private void runIndexTask(Long taskId) {
        if (!startTaskIfPending(taskId)) {
            return;
        }
        KnowledgeIndexTask task = loadTaskWithRelations(taskId);
        Long knowledgeBaseId = task.getKnowledgeBaseId();
        try {
            runTask(task);
            markTaskTerminal(taskId, KnowledgeIndexTask.Status.SUCCESS, null);
        } catch (Exception ex) {
            String error = buildTaskFailureMessage(task, ex);
            log.error("Knowledge index task {} failed: {}", taskId, error, ex);
            markTaskTerminal(taskId, KnowledgeIndexTask.Status.FAILED, error);
            if (task.getDocumentId() != null) {
                markDocumentFailed(task.getDocumentId(), error);
            }
        } finally {
            recoverKnowledgeBaseStatus(knowledgeBaseId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexTask> listKnowledgeBaseTasks(Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return knowledgeIndexTaskRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexTask> listDocumentTasks(Long documentId) {
        KnowledgeDocument document = knowledgeAccessGuard.requireDocumentRead(documentId);
        return knowledgeIndexTaskRepository.findByDocument_IdOrderByCreatedAtDesc(document.getId());
    }

    private boolean startTaskIfPending(Long taskId) {
        Instant now = Instant.now();
        int updated = knowledgeIndexTaskRepository.transitionStatusToRunning(
                taskId,
                KnowledgeIndexTask.Status.PENDING,
                KnowledgeIndexTask.Status.RUNNING,
                now,
                now
        );
        if (updated > 0) {
            return true;
        }
        KnowledgeIndexTask current = knowledgeIndexTaskRepository.findById(taskId).orElse(null);
        if (current != null) {
            log.info("Skip knowledge index task {} because current status is {}", taskId, current.getStatus());
        }
        return false;
    }

    private void markTaskTerminal(Long taskId, KnowledgeIndexTask.Status targetStatus, String errorMessage) {
        Instant now = Instant.now();
        int updated = knowledgeIndexTaskRepository.transitionStatusFromRunning(
                taskId,
                KnowledgeIndexTask.Status.RUNNING,
                targetStatus,
                fitErrorMessage(errorMessage),
                now,
                now
        );
        if (updated == 0) {
            KnowledgeIndexTask current = knowledgeIndexTaskRepository.findById(taskId).orElse(null);
            if (current != null) {
                log.info("Skip terminal transition for task {} because current status is {}", taskId, current.getStatus());
            }
        }
    }

    private void failBeforeStart(Long taskId, String errorMessage) {
        Instant now = Instant.now();
        int started = knowledgeIndexTaskRepository.transitionStatusToRunning(
                taskId,
                KnowledgeIndexTask.Status.PENDING,
                KnowledgeIndexTask.Status.RUNNING,
                now,
                now
        );
        if (started > 0) {
            markTaskTerminal(taskId, KnowledgeIndexTask.Status.FAILED, errorMessage);
        }
    }

    private void markDocumentFailed(Long documentId, String errorMessage) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId).orElse(null);
        if (document == null) {
            return;
        }
        document.setStatus(KnowledgeDocument.Status.FAILED);
        document.setErrorMessage(fitErrorMessage(errorMessage));
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private String buildTaskFailureMessage(KnowledgeIndexTask task, Exception ex) {
        String detail = trimError(ex == null ? null : ex.getMessage());
        KnowledgeIndexTask.TaskType taskType = task == null || task.getTaskType() == null
                ? KnowledgeIndexTask.TaskType.REINDEX
                : task.getTaskType();
        String phase = switch (taskType) {
            case PARSE -> "PARSE";
            case CHUNK -> "CHUNK";
            case EMBED -> "EMBED";
            case REINDEX -> "REINDEX";
        };
        return StringUtils.hasText(detail) ? phase + "阶段失败: " + detail : phase + "阶段失败";
    }

    private KnowledgeIndexTask loadTaskWithRelations(Long taskId) {
        return knowledgeIndexTaskRepository.findByIdWithRelations(taskId)
                .orElseThrow(() -> new RuntimeException("Knowledge index task not found"));
    }

    private void recoverKnowledgeBaseStatus(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            return;
        }
        try {
            refreshKnowledgeBaseStats(knowledgeBaseId, null);
        } catch (Exception ex) {
            log.warn("Failed to recover knowledge base {} status after task execution: {}", knowledgeBaseId, ex.getMessage());
        }
    }

    private void runTask(KnowledgeIndexTask task) {
        KnowledgeBase knowledgeBase = task.getKnowledgeBase();
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("Knowledge base not found");
        }
        knowledgeBase.setStatus(KnowledgeBase.Status.INDEXING);
        knowledgeBase.setUpdatedAt(Instant.now());
        knowledgeBaseRepository.save(knowledgeBase);

        if (task.getDocument() != null) {
            runTaskOnDocument(knowledgeBase, loadDocument(knowledgeBase.getId(), task.getDocument().getId()), task.getTaskType());
        } else {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBase.getId());
            for (KnowledgeDocument document : documents) {
                if (document.getStatus() == KnowledgeDocument.Status.DISABLED) {
                    continue;
                }
                runTaskOnDocument(knowledgeBase, document, task.getTaskType());
            }
        }

        refreshKnowledgeBaseStats(knowledgeBase.getId(), KnowledgeBase.Status.ACTIVE);
    }

    private void runTaskOnDocument(KnowledgeBase knowledgeBase, KnowledgeDocument managedDocument, KnowledgeIndexTask.TaskType taskType) {
        switch (taskType == null ? KnowledgeIndexTask.TaskType.REINDEX : taskType) {
            case PARSE -> parseDocument(managedDocument);
            case CHUNK -> {
                parseDocument(managedDocument);
                RebuildChunksResult rebuildResult = rebuildChunks(knowledgeBase, managedDocument);
                ensureChunksBuilt(rebuildResult);
                rebuildCodeIndex(knowledgeBase, managedDocument, rebuildResult);
            }
            case EMBED -> {
                parseDocument(managedDocument);
                List<KnowledgeChunk> existingChunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(managedDocument.getId());
                if (existingChunks.isEmpty()) {
                    RebuildChunksResult rebuildResult = rebuildChunks(knowledgeBase, managedDocument);
                    ensureChunksBuilt(rebuildResult);
                    rebuildCodeIndex(knowledgeBase, managedDocument, rebuildResult);
                }
                knowledgeEmbeddingService.backfillDocumentEmbeddings(
                        managedDocument.getId(),
                        knowledgeBase.getEmbeddingProvider(),
                        knowledgeBase.getEmbeddingModel(),
                        null
                );
                markDocumentIndexed(managedDocument);
            }
            case REINDEX -> {
                parseDocument(managedDocument);
                RebuildChunksResult rebuildResult = rebuildChunks(knowledgeBase, managedDocument);
                ensureChunksBuilt(rebuildResult);
                rebuildCodeIndex(knowledgeBase, managedDocument, rebuildResult);
                knowledgeEmbeddingService.backfillDocumentEmbeddings(
                        managedDocument.getId(),
                        knowledgeBase.getEmbeddingProvider(),
                        knowledgeBase.getEmbeddingModel(),
                        null
                );
                markDocumentIndexed(managedDocument);
            }
        }
    }

    private void ensureChunksBuilt(RebuildChunksResult rebuildResult) {
        if (rebuildResult == null || rebuildResult.chunks().isEmpty()) {
            throw new IllegalStateException("Chunking produced no chunks");
        }
    }

    private void parseDocument(KnowledgeDocument document) {
        String content = normalizeContent(document.getContentText());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("鏂囨。鍐呭涓嶈兘涓虹┖");
        }
        String filePath = resolveDocumentPath(document);
        document.setTitle(resolveDocumentTitle(document.getTitle(), document.getFileName(), content));
        document.setContentText(content);
        document.setContentHash(StringUtils.hasText(document.getContentHash()) ? document.getContentHash().trim() : sha256(content));
        document.setFilePath(filePath);
        document.setDocKind(resolveDocKind(filePath));
        document.setParserName("knowledge-chunking");
        document.setParserVersion("semantic-v3");
        document.setParseStatus(KnowledgeDocument.ParseStatus.PARSED);
        if (document.getSymbolIndexStatus() == null) {
            document.setSymbolIndexStatus(KnowledgeDocument.SymbolIndexStatus.PENDING);
        }
        if (document.getSymbolCount() == null) {
            document.setSymbolCount(0);
        }
        if (document.getReferenceCount() == null) {
            document.setReferenceCount(0);
        }
        document.setErrorMessage(null);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private RebuildChunksResult rebuildChunks(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        codeIndexService.clearDocumentCodeIndex(document.getId());
        knowledgeChunkEmbeddingRepository.deleteByDocumentId(document.getId());
        knowledgeChunkRepository.deleteByDocumentId(document.getId());
        knowledgeChunkRepository.flush();

        KnowledgeChunkingService.IndexBuildResult draft = knowledgeChunkingService.buildIndexDraft(
                knowledgeBase,
                document,
                document.getContentText(),
                null
        );
        List<KnowledgeChunkingService.ChunkDraft> parts = draft.chunks();
        if (parts.isEmpty()) {
            document.setStatus(KnowledgeDocument.Status.FAILED);
            document.setParseStatus(KnowledgeDocument.ParseStatus.FAILED);
            document.setSymbolIndexStatus(KnowledgeDocument.SymbolIndexStatus.FAILED);
            document.setSymbolCount(0);
            document.setReferenceCount(0);
            document.setErrorMessage("鍒囩墖缁撴灉涓虹┖");
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
            return new RebuildChunksResult(List.of(), draft);
        }

        List<KnowledgeChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < parts.size(); i++) {
            KnowledgeChunkingService.ChunkDraft part = parts.get(i);
            String textPart = normalizeContent(part.content());
            if (!StringUtils.hasText(textPart)) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeBase(knowledgeBase);
            chunk.setDocument(document);
            chunk.setChunkIndex(part.chunkIndex() == null ? i : part.chunkIndex());
            chunk.setChunkType(resolveChunkType(part.language()));
            chunk.setContent(textPart);
            chunk.setTokenCount(part.tokenCount() == null ? estimateTokens(textPart) : part.tokenCount());
            chunk.setFilePath(StringUtils.hasText(part.path()) ? part.path() : document.getFilePath());
            chunk.setStartLine(part.startLine());
            chunk.setStartColumn(null);
            chunk.setEndLine(part.endLine());
            chunk.setEndColumn(null);
            chunk.setSectionPath(part.sectionName());
            chunk.setContentHash(sha256(textPart));
            chunk.setMetadataJson(part.metadataJson());
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            chunks.add(chunk);
        }

        List<KnowledgeChunk> savedChunks = List.of();
        if (!chunks.isEmpty()) {
            savedChunks = knowledgeChunkRepository.saveAll(chunks);
        }

        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        document.setParseStatus(KnowledgeDocument.ParseStatus.PARSED);
        document.setSymbolIndexStatus(KnowledgeDocument.SymbolIndexStatus.PENDING);
        document.setErrorMessage(null);
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
        return new RebuildChunksResult(savedChunks, draft);
    }

    private void rebuildCodeIndex(KnowledgeBase knowledgeBase,
                                  KnowledgeDocument document,
                                  RebuildChunksResult rebuildResult) {
        CodeIndexService.CodeIndexResult indexResult = codeIndexService.rebuildDocumentCodeIndex(
                knowledgeBase,
                document,
                rebuildResult.chunks(),
                rebuildResult.indexDraft()
        );
        document.setSymbolCount(indexResult.symbolCount());
        document.setReferenceCount(indexResult.referenceCount());
        document.setSymbolIndexStatus(indexResult.symbolIndexStatus());
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private void markDocumentIndexed(KnowledgeDocument document) {
        document.setStatus(KnowledgeDocument.Status.INDEXED);
        if (document.getParseStatus() == null) {
            document.setParseStatus(KnowledgeDocument.ParseStatus.PARSED);
        }
        if (document.getSymbolIndexStatus() == null) {
            document.setSymbolIndexStatus(KnowledgeDocument.SymbolIndexStatus.NOT_APPLICABLE);
        }
        document.setErrorMessage(null);
        document.setIndexedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private KnowledgeDocument uploadSingleDocument(KnowledgeBase knowledgeBase,
                                                   MultipartFile file,
                                                   KnowledgeDocumentCreateRequest request,
                                                   boolean singleFile) {
        String originalFileName = sanitizeFileName(file.getOriginalFilename());
        Instant now = Instant.now();

        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(resolveUploadSourceType(request));
        entity.setSourceRefId(request == null ? null : request.getSourceRefId());
        entity.setSourceUrl(trimToNull(request == null ? null : request.getSourceUrl()));
        entity.setFileName(originalFileName);
        entity.setMimeType(resolveMimeType(file, originalFileName));
        entity.setLanguage(trimToNull(request == null ? null : request.getLanguage()));
        entity.setUploadedBy(request == null ? null : request.getUploadedBy());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            Path storedPath = storeUploadedFile(knowledgeBase.getId(), file, originalFileName);
            String extractedText = extractTextFromFile(storedPath, originalFileName, entity.getMimeType());
            String normalizedText = normalizeContent(extractedText);

            if (!StringUtils.hasText(normalizedText)) {
                entity.setTitle(resolveDocumentTitle(request == null ? null : request.getTitle(), originalFileName, ""));
                entity.setStoragePath(toRelativeStoragePath(storedPath));
                entity.setStatus(KnowledgeDocument.Status.FAILED);
                entity.setErrorMessage("Unable to extract text from file");
                return knowledgeDocumentRepository.save(entity);
            }

            entity.setTitle(resolveDocumentTitle(singleFile && request != null ? request.getTitle() : null, originalFileName, normalizedText));
            entity.setStoragePath(toRelativeStoragePath(storedPath));
            entity.setContentText(normalizedText);
            entity.setContentHash(sha256(normalizedText));
            entity.setErrorMessage(null);
            entity.setStatus(KnowledgeDocument.Status.UPLOADED);

            return persistDocumentAndMaybeIndex(knowledgeBase, entity, shouldAutoIndex(request));
        } catch (Exception ex) {
            entity.setTitle(resolveDocumentTitle(request == null ? null : request.getTitle(), originalFileName, ""));
            entity.setStatus(KnowledgeDocument.Status.FAILED);
            entity.setErrorMessage(trimError(ex.getMessage()));
            return knowledgeDocumentRepository.save(entity);
        }
    }

    private KnowledgeDocument importExtractedProjectFile(KnowledgeBase knowledgeBase,
                                                         Path extractedPath,
                                                         String entryPath,
                                                         String zipName,
                                                         KnowledgeDocumentCreateRequest request) {
        String fileName = sanitizeFileName(extractedPath.getFileName().toString());
        Instant now = Instant.now();

        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(KnowledgeDocument.SourceType.PROJECT_DOC);
        entity.setSourceRefId(request == null ? null : request.getSourceRefId());
        entity.setArchiveEntryPath(entryPath);
        entity.setSourceUrl(StringUtils.hasText(entryPath) ? entryPath : trimToNull(request == null ? null : request.getSourceUrl()));
        entity.setFileName(fileName);
        entity.setTitle(StringUtils.hasText(entryPath) ? entryPath : fileName);
        entity.setMimeType(resolveMimeType(extractedPath, fileName, MediaType.APPLICATION_OCTET_STREAM_VALUE));
        entity.setLanguage(trimToNull(request == null ? null : request.getLanguage()));
        entity.setUploadedBy(request == null ? null : request.getUploadedBy());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        try {
            Path storedPath = storePathFile(knowledgeBase.getId(), extractedPath, fileName);
            String extractedText = extractTextFromFile(storedPath, fileName, entity.getMimeType());
            String normalizedText = normalizeContent(extractedText);

            if (!StringUtils.hasText(normalizedText)) {
                entity.setStoragePath(toRelativeStoragePath(storedPath));
                entity.setStatus(KnowledgeDocument.Status.FAILED);
                entity.setErrorMessage("Unable to extract text from project file");
                return knowledgeDocumentRepository.save(entity);
            }

            entity.setStoragePath(toRelativeStoragePath(storedPath));
            entity.setContentText(normalizedText);
            entity.setContentHash(sha256(normalizedText));
            entity.setErrorMessage(null);
            entity.setStatus(KnowledgeDocument.Status.UPLOADED);

            return persistDocumentAndMaybeIndex(knowledgeBase, entity, true);
        } catch (Exception ex) {
            entity.setStatus(KnowledgeDocument.Status.FAILED);
            entity.setErrorMessage(trimError(ex.getMessage()));
            return knowledgeDocumentRepository.save(entity);
        }
    }

    private KnowledgeDocument persistDocumentAndMaybeIndex(KnowledgeBase knowledgeBase,
                                                           KnowledgeDocument entity,
                                                           boolean autoIndex) {
        KnowledgeDocument saved = knowledgeDocumentRepository.save(entity);
        refreshKnowledgeBaseStats(knowledgeBase.getId(), null);

        if (!autoIndex) {
            return knowledgeDocumentRepository.findById(saved.getId()).orElse(saved);
        }

        KnowledgeIndexTask autoTask = createPendingIndexTask(knowledgeBase, saved, KnowledgeIndexTask.TaskType.REINDEX);
        submitIndexTask(autoTask.getId());

        return knowledgeDocumentRepository.findById(saved.getId()).orElse(saved);
    }

    private List<KnowledgeDocument> loadDownloadDocuments(Long knowledgeBaseId, Collection<Long> documentIds) {
        if (documentIds == null || documentIds.isEmpty()) {
            return knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);
        }
        return knowledgeDocumentRepository.findByKnowledgeBase_IdAndIdInOrderByIdAsc(knowledgeBaseId, documentIds);
    }

    private KnowledgeDocument.SourceType resolveUploadSourceType(KnowledgeDocumentCreateRequest request) {
        if (request == null || request.getSourceType() == null) {
            return KnowledgeDocument.SourceType.UPLOAD;
        }
        return request.getSourceType();
    }

    private boolean shouldAutoIndex(KnowledgeDocumentCreateRequest request) {
        return request == null || request.getAutoIndex() == null || request.getAutoIndex();
    }

    private Path storeUploadedFile(Long knowledgeBaseId, MultipartFile file, String originalFileName) throws IOException {
        Path baseDir = ensureStorageDirectory();
        String safeName = sanitizeFileName(originalFileName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "-" + safeName;

        Path targetDir = baseDir.resolve(String.valueOf(knowledgeBaseId));
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedName).normalize();
        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
        }
        return targetPath;
    }

    private Path storePathFile(Long knowledgeBaseId, Path sourcePath, String originalFileName) throws IOException {
        Path baseDir = ensureStorageDirectory();
        String safeName = sanitizeFileName(originalFileName);
        String storedName = UUID.randomUUID().toString().replace("-", "") + "-" + safeName;

        Path targetDir = baseDir.resolve(String.valueOf(knowledgeBaseId));
        Files.createDirectories(targetDir);

        Path targetPath = targetDir.resolve(storedName).normalize();
        Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);
        return targetPath;
    }

    private Path ensureStorageDirectory() throws IOException {
        Path baseDir = Paths.get(storageRoot).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        return baseDir;
    }

    private String toRelativeStoragePath(Path storedPath) throws IOException {
        Path baseDir = ensureStorageDirectory();
        return baseDir.relativize(storedPath.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private Path resolveStoredPath(String storagePath) throws IOException {
        if (!StringUtils.hasText(storagePath)) {
            throw new RuntimeException("閺傚洦銆傚▽鈩冩箒閸欘垯绗呮潪鐣屾畱閸樼喎顫愰弬鍥︽");
        }
        Path baseDir = ensureStorageDirectory();
        Path path = baseDir.resolve(storagePath).normalize();
        if (!path.startsWith(baseDir)) {
            throw new RuntimeException("Invalid storage path");
        }
        if (!Files.exists(path)) {
            throw new RuntimeException("Original file not found");
        }
        return path;
    }

    private void deleteStoredDocumentFile(String storagePath) {
        if (!StringUtils.hasText(storagePath)) {
            return;
        }
        try {
            Path baseDir = ensureStorageDirectory();
            Path path = baseDir.resolve(storagePath).normalize();
            if (!path.startsWith(baseDir)) {
                log.warn("Skip deleting knowledge document file outside storage root: {}", storagePath);
                return;
            }
            Files.deleteIfExists(path);
        } catch (IOException ex) {
            log.warn("Failed to delete stored knowledge document file {}: {}", storagePath, ex.getMessage());
        }
    }

    private String extractTextFromFile(Path storedPath, String originalFileName, String mimeType) throws IOException {
        String ext = getExtension(originalFileName);
        if ("pdf".equals(ext)) {
            try (PDDocument document = Loader.loadPDF(Files.readAllBytes(storedPath))) {
                return new PDFTextStripper().getText(document);
            }
        }
        if ("docx".equals(ext)) {
            try (InputStream inputStream = Files.newInputStream(storedPath);
                 XWPFDocument document = new XWPFDocument(inputStream);
                 XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
                return extractor.getText();
            }
        }
        if ("doc".equals(ext)) {
            try (InputStream inputStream = Files.newInputStream(storedPath);
                 HWPFDocument document = new HWPFDocument(inputStream);
                 WordExtractor extractor = new WordExtractor(document)) {
                return extractor.getText();
            }
        }
        if (isTextLikeExtension(ext) || isTextLikeMimeType(mimeType)) {
            return stripUtf8Bom(Files.readString(storedPath, StandardCharsets.UTF_8));
        }
        return "";
    }

    private boolean isTextLikeMimeType(String mimeType) {
        if (!StringUtils.hasText(mimeType)) return false;
        String lower = mimeType.toLowerCase(Locale.ROOT);
        return lower.startsWith("text/")
                || lower.contains("json")
                || lower.contains("xml")
                || lower.contains("javascript")
                || lower.contains("typescript")
                || lower.contains("sql")
                || lower.contains("yaml");
    }

    private boolean isTextLikeExtension(String ext) {
        return Set.of(
                "txt", "md", "markdown", "json", "csv", "js", "ts", "java", "xml", "html", "htm",
                "css", "vue", "sql", "yml", "yaml", "properties", "log", "text"
        ).contains(ext);
    }

    private boolean isImportableExtension(String ext) {
        return Set.of(
                "txt", "md", "markdown", "json", "csv", "js", "ts", "java", "xml", "html", "htm",
                "css", "vue", "sql", "yml", "yaml", "properties", "log", "text", "pdf", "doc", "docx"
        ).contains(ext);
    }

    private String stripUtf8Bom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\ufeff') {
            return text.substring(1);
        }
        return text;
    }

    private String sanitizeFileName(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return "unnamed.txt";
        }
        String sanitized = fileName.replace("\\", "/");
        sanitized = sanitized.substring(sanitized.lastIndexOf('/') + 1).trim();
        sanitized = sanitized.replaceAll("[\r\n]", "");
        sanitized = sanitized.replaceAll("[^\u4e00-\u9fa5a-zA-Z0-9._-]", "_");
        return sanitized.isBlank() ? "unnamed.txt" : sanitized;
    }

    private String getExtension(String fileName) {
        if (!StringUtils.hasText(fileName) || !fileName.contains(".")) {
            return "";
        }
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase(Locale.ROOT);
    }

    private String resolveMimeType(MultipartFile file, String fileName) {
        String contentType = file == null ? null : trimToNull(file.getContentType());
        if (StringUtils.hasText(contentType)) {
            return contentType;
        }
        return resolveMimeType(null, fileName, null);
    }

    private String resolveMimeType(Path path, String fileName, String fallback) {
        try {
            String detected = path != null ? Files.probeContentType(path) : null;
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
        }

        String ext = getExtension(fileName);
        return switch (ext) {
            case "pdf" -> "application/pdf";
            case "doc" -> "application/msword";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            case "md", "markdown" -> "text/markdown";
            case "json" -> "application/json";
            case "xml" -> "application/xml";
            case "html", "htm" -> "text/html";
            case "css" -> "text/css";
            case "js" -> "application/javascript";
            case "ts" -> "application/typescript";
            case "java", "txt", "text", "log", "properties", "sql", "yml", "yaml", "vue" -> "text/plain";
            default -> StringUtils.hasText(fallback) ? fallback : MediaType.APPLICATION_OCTET_STREAM_VALUE;
        };
    }

    private String normalizeContent(String text) {
        if (text == null) {
            return null;
        }
        String normalized = stripUtf8Bom(text)
                .replace("\r\n", "\n")
                .replace("\r", "\n");
        normalized = normalized.replaceAll("\u0000", "");
        normalized = normalized.replaceAll("[\\t\\u000B\\f]+", " ");
        normalized = normalized.replaceAll("(?m)[ \t]+$", "");
        normalized = normalized.replaceAll("\n{3,}", "\n\n").trim();
        return normalized;
    }

    private String resolveDocumentTitle(String title, String fileName, String contentText) {
        if (StringUtils.hasText(title)) {
            return trimToNull(title);
        }
        if (StringUtils.hasText(fileName)) {
            return sanitizeFileName(fileName);
        }
        if (StringUtils.hasText(contentText)) {
            String firstLine = normalizeContent(contentText);
            if (StringUtils.hasText(firstLine)) {
                int idx = firstLine.indexOf('\n');
                String candidate = idx >= 0 ? firstLine.substring(0, idx) : firstLine;
                candidate = candidate.trim();
                if (candidate.length() > 120) {
                    candidate = candidate.substring(0, 120);
                }
                if (StringUtils.hasText(candidate)) {
                    return candidate;
                }
            }
        }
        return "unnamed-document";
    }

    private String ensureDownloadFileName(KnowledgeDocument document) {
        String candidate = trimToNull(document.getFileName());
        if (!StringUtils.hasText(candidate)) {
            candidate = ensureTextFileName(resolveDocumentTitle(document.getTitle(), null, document.getContentText()));
        }
        return sanitizeFileName(candidate);
    }

    private String ensureTextFileName(String fileName) {
        String safe = sanitizeFileName(fileName);
        return safe.contains(".") ? safe : safe + ".txt";
    }

    private String ensureUniqueFileName(Set<String> usedNames, String fileName) {
        String safe = sanitizeFileName(fileName);
        if (usedNames.add(safe)) {
            return safe;
        }
        String ext = "";
        String name = safe;
        int idx = safe.lastIndexOf('.');
        if (idx > 0) {
            ext = safe.substring(idx);
            name = safe.substring(0, idx);
        }
        int seq = 2;
        while (true) {
            String candidate = name + "_" + seq + ext;
            if (usedNames.add(candidate)) {
                return candidate;
            }
            seq++;
        }
    }

    private String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest((raw == null ? "" : raw).getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception ex) {
            throw new RuntimeException("鐠侊紕鐣婚崘鍛啇閸濆牆绗囨径杈Е", ex);
        }
    }

    private void applyEmbeddingConfig(KnowledgeBase entity, String embeddingProvider, String embeddingModel) {
        String provider = normalizeEmbeddingProvider(embeddingProvider);
        String model = normalizeEmbeddingModel(embeddingModel);
        if (StringUtils.hasText(provider) ^ StringUtils.hasText(model)) {
            throw new IllegalArgumentException("Embedding provider and model must be configured together");
        }
        entity.setEmbeddingProvider(provider);
        entity.setEmbeddingModel(model);
    }

    private String normalizeEmbeddingProvider(String value) {
        return EmbeddingNameNormalizer.normalizeProvider(value);
    }

    private String normalizeEmbeddingModel(String value) {
        return EmbeddingNameNormalizer.normalizeModel(value);
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String trimError(String error) {
        if (!StringUtils.hasText(error)) {
            return "閺堫亞鐓￠柨娆掝嚖";
        }
        String trimmed = error.replaceAll("[\r\n]+", " ").trim();
        return trimmed.length() > ERROR_MESSAGE_MAX_LENGTH
                ? trimmed.substring(0, ERROR_MESSAGE_MAX_LENGTH)
                : trimmed;
    }

    private String fitErrorMessage(String errorMessage) {
        String normalized = trimToNull(errorMessage);
        if (!StringUtils.hasText(normalized)) {
            return null;
        }
        return normalized.length() > ERROR_MESSAGE_MAX_LENGTH
                ? normalized.substring(0, ERROR_MESSAGE_MAX_LENGTH)
                : normalized;
    }

    private int estimateTokens(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    private String resolveDocumentPath(KnowledgeDocument document) {
        if (document == null) {
            return "";
        }
        if (StringUtils.hasText(document.getFilePath())) {
            return document.getFilePath();
        }
        if (StringUtils.hasText(document.getArchiveEntryPath())) {
            return document.getArchiveEntryPath();
        }
        if (StringUtils.hasText(document.getSourceUrl())) {
            return document.getSourceUrl();
        }
        if (StringUtils.hasText(document.getFileName())) {
            return document.getFileName();
        }
        return "";
    }

    private KnowledgeDocument.DocKind resolveDocKind(String path) {
        String lower = path == null ? "" : path.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".java")
                || lower.endsWith(".js")
                || lower.endsWith(".jsx")
                || lower.endsWith(".ts")
                || lower.endsWith(".tsx")
                || lower.endsWith(".vue")
                || lower.endsWith(".sql")
                || lower.endsWith(".xml")
                || lower.endsWith(".json")
                || lower.endsWith(".yml")
                || lower.endsWith(".yaml")
                || lower.endsWith(".css")
                || lower.endsWith(".scss")
                || lower.endsWith(".less")
                || lower.endsWith(".html")
                || lower.endsWith(".htm")) {
            return KnowledgeDocument.DocKind.CODE_FILE;
        }
        if (lower.endsWith("readme") || lower.contains("/readme.") || lower.startsWith("readme.")) {
            return KnowledgeDocument.DocKind.README;
        }
        return KnowledgeDocument.DocKind.DOCUMENT;
    }

    private KnowledgeChunk.ChunkType resolveChunkType(String language) {
        if (!StringUtils.hasText(language)) {
            return KnowledgeChunk.ChunkType.TEXT;
        }
        String lower = language.toLowerCase(Locale.ROOT);
        if ("java".equals(lower)
                || "javascript".equals(lower)
                || "typescript".equals(lower)
                || "jsx".equals(lower)
                || "tsx".equals(lower)
                || "vue".equals(lower)
                || "sql".equals(lower)) {
            return KnowledgeChunk.ChunkType.CODE_BLOCK;
        }
        return KnowledgeChunk.ChunkType.TEXT;
    }

    private KnowledgeBase findKnowledgeBaseRequired(Long knowledgeBaseId) {
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found"));
    }

    private KnowledgeDocument loadDocument(Long knowledgeBaseId, Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("Knowledge document not found"));
        if (!document.getKnowledgeBase().getId().equals(knowledgeBaseId)) {
            throw new RuntimeException("Document does not belong to this knowledge base");
        }
        return document;
    }

    private void ensureOwnerMember(KnowledgeBase knowledgeBase) {
        if (knowledgeBase.getOwnerId() == null) {
            return;
        }
        if (knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(knowledgeBase.getId(), knowledgeBase.getOwnerId())) {
            return;
        }

        KnowledgeBaseMember entity = new KnowledgeBaseMember();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setUserId(knowledgeBase.getOwnerId());
        entity.setRoleCode(KnowledgeBaseMember.RoleCode.OWNER);
        entity.setGrantedBy(knowledgeBase.getOwnerId());
        entity.setCreatedAt(Instant.now());
        knowledgeBaseMemberRepository.save(entity);
    }

    private void validateKnowledgeBaseRequest(KnowledgeBaseCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Knowledge base request cannot be null");
        }
        KnowledgeBase.ScopeType scopeType = knowledgeBaseScopePolicy.normalizeScope(request.getScopeType());
        knowledgeBaseScopePolicy.validateScopeBinding(scopeType, request.getOwnerId(), request.getProjectId());
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("Knowledge base name cannot be blank");
        }
    }

    private void validateDocumentRequest(KnowledgeDocumentCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("閺傚洦銆傜拠閿嬬湴娑撳秷鍏樻稉铏光敄");
        }
        if (!StringUtils.hasText(request.getTitle())
                && !StringUtils.hasText(request.getFileName())
                && !StringUtils.hasText(request.getContentText())) {
            throw new IllegalArgumentException("title, fileName or contentText is required");
        }
        if (!StringUtils.hasText(request.getContentText())) {
            throw new IllegalArgumentException("contentText娑撳秷鍏樻稉铏光敄");
        }
    }

    private KnowledgeIndexTask.TaskType resolveTaskType(KnowledgeIndexTaskCreateRequest request) {
        return request == null || request.getTaskType() == null ? KnowledgeIndexTask.TaskType.REINDEX : request.getTaskType();
    }

    private void refreshKnowledgeBaseStats(Long knowledgeBaseId, KnowledgeBase.Status status) {
        KnowledgeBase knowledgeBase = findKnowledgeBaseRequired(knowledgeBaseId);
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);

        int docCount = docs.size();
        int chunkCount = 0;
        Instant lastIndexedAt = null;
        boolean hasProcessingDocument = false;
        boolean hasFailedDocument = false;
        for (KnowledgeDocument doc : docs) {
            List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(doc.getId());
            chunkCount += chunks.size();
            if (doc.getIndexedAt() != null && (lastIndexedAt == null || doc.getIndexedAt().isAfter(lastIndexedAt))) {
                lastIndexedAt = doc.getIndexedAt();
            }
            if (doc.getStatus() == KnowledgeDocument.Status.UPLOADED || doc.getStatus() == KnowledgeDocument.Status.PARSING) {
                hasProcessingDocument = true;
            }
            if (doc.getStatus() == KnowledgeDocument.Status.FAILED) {
                hasFailedDocument = true;
            }
        }

        knowledgeBase.setDocCount(docCount);
        knowledgeBase.setChunkCount(chunkCount);
        knowledgeBase.setLastIndexedAt(lastIndexedAt);
        if (status != null) {
            knowledgeBase.setStatus(status);
        } else if (knowledgeBase.getStatus() == KnowledgeBase.Status.DISABLED
                || knowledgeBase.getStatus() == KnowledgeBase.Status.ARCHIVED) {
            // Preserve admin-managed terminal states when background refresh updates counters.
        } else if (docCount == 0) {
            knowledgeBase.setStatus(KnowledgeBase.Status.DRAFT);
        } else if (hasProcessingDocument) {
            knowledgeBase.setStatus(KnowledgeBase.Status.INDEXING);
        } else if (hasFailedDocument && chunkCount == 0) {
            knowledgeBase.setStatus(KnowledgeBase.Status.FAILED);
        } else {
            knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
        }
        knowledgeBase.setUpdatedAt(Instant.now());
        knowledgeBaseRepository.save(knowledgeBase);
    }

    private String normalizeZipEntryName(String raw) {
        if (!StringUtils.hasText(raw)) {
            return null;
        }
        String s = raw.replace("\\", "/").trim();
        while (s.startsWith("/")) {
            s = s.substring(1);
        }
        return s;
    }

    private long copyZipEntry(ZipInputStream zis, Path targetPath, long maxEntryBytes) throws IOException {
        long total = 0L;
        byte[] buffer = new byte[8192];
        try (OutputStream os = Files.newOutputStream(targetPath)) {
            int len;
            while ((len = zis.read(buffer)) > 0) {
                total += len;
                if (total > maxEntryBytes) {
                    throw new RuntimeException("ZIP 閸愬懎宕熸稉顏呮瀮娴犳儼绻冩径? " + targetPath.getFileName());
                }
                os.write(buffer, 0, len);
            }
        }
        return total;
    }

    private boolean shouldSkipProjectEntry(String entryPath) {
        String s = entryPath.toLowerCase(Locale.ROOT);
        return s.startsWith(".git/")
                || s.contains("/.git/")
                || s.startsWith("node_modules/")
                || s.contains("/node_modules/")
                || s.startsWith("target/")
                || s.contains("/target/")
                || s.startsWith("dist/")
                || s.contains("/dist/")
                || s.startsWith("build/")
                || s.contains("/build/")
                || s.startsWith(".idea/")
                || s.contains("/.idea/")
                || s.startsWith(".vscode/")
                || s.contains("/.vscode/")
                || s.endsWith(".png")
                || s.endsWith(".jpg")
                || s.endsWith(".jpeg")
                || s.endsWith(".gif")
                || s.endsWith(".webp")
                || s.endsWith(".mp4")
                || s.endsWith(".mp3")
                || s.endsWith(".class")
                || s.endsWith(".jar")
                || s.endsWith(".exe");
    }

    private record RebuildChunksResult(
            List<KnowledgeChunk> chunks,
            KnowledgeChunkingService.IndexBuildResult indexDraft
    ) {
    }
}


