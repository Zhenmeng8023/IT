package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.config.AiKnowledgeTaskExecutorConfig;
import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkEmbeddingRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.CodeIndexService;
import com.alikeyou.itmoduleai.service.KnowledgeAccessGuard;
import com.alikeyou.itmoduleai.service.KnowledgeChunkingService;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.util.FileSystemUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class KnowledgeImportTaskServiceImpl implements KnowledgeImportTaskService {

    private static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;
    private static final int MAX_IMPORTABLE_FILES = 1500;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private final KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private final KnowledgeChunkingService knowledgeChunkingService;
    private final CodeIndexService codeIndexService;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final KnowledgeAccessGuard knowledgeAccessGuard;
    @Qualifier(AiKnowledgeTaskExecutorConfig.AI_KNOWLEDGE_TASK_EXECUTOR)
    private final Executor aiKnowledgeTaskExecutor;

    @Value("${app.ai.knowledge-document.storage-root:${user.dir}/.runtime/knowledge-documents}")
    private String storageRoot;

    @Value("${app.ai.knowledge-import.storage-root:${user.dir}/.runtime/knowledge-imports}")
    private String importRoot;

    @Override
    @Transactional
    public KnowledgeImportTask createZipImportTask(Long knowledgeBaseId, MultipartFile file, KnowledgeDocumentCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeAccessGuard.requireKnowledgeBaseEdit(knowledgeBaseId);

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP file must not be empty");
        }

        String zipName = sanitizeFileName(file.getOriginalFilename());
        if (!zipName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("Only .zip files are supported");
        }

        try {
            Path taskDir = ensureImportDirectory().resolve(UUID.randomUUID().toString().replace("-", ""));
            Files.createDirectories(taskDir);
            Path zipPath = taskDir.resolve(zipName).normalize();
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, zipPath, StandardCopyOption.REPLACE_EXISTING);
            }

            KnowledgeImportTask task = new KnowledgeImportTask();
            task.setKnowledgeBase(knowledgeBase);
            task.setZipName(zipName);
            task.setTempZipPath(zipPath.toAbsolutePath().normalize().toString());
            task.setStatus(KnowledgeImportTask.Status.PENDING);
            task.setCurrentStage(KnowledgeImportTask.Stage.UPLOADED);
            task.setTotalFiles(0);
            task.setScannedFiles(0);
            task.setImportedFiles(0);
            task.setSkippedFiles(0);
            task.setFailedFiles(0);
            task.setProgressPercent(0);
            task.setCurrentFile(null);
            task.setErrorMessage(null);
            task.setCancelRequested(Boolean.FALSE);
            task.setCreatedBy(request == null ? null : request.getUploadedBy());
            Instant now = Instant.now();
            task.setCreatedAt(now);
            task.setUpdatedAt(now);
            task.setFinishedAt(null);

            KnowledgeImportTask saved = knowledgeImportTaskRepository.save(task);
            KnowledgeDocumentCreateRequest safeRequest = cloneRequest(request);
            submitImportTask(saved.getId(), safeRequest);
            return saved;
        } catch (IOException ex) {
            throw new RuntimeException("娣囨繂鐡?ZIP 閺傚洣娆㈡径杈Е: " + trimError(ex.getMessage()), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeImportTask getTask(Long taskId) {
        return knowledgeAccessGuard.requireImportTaskRead(taskId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeImportTask> listByKnowledgeBase(Long knowledgeBaseId) {
        knowledgeAccessGuard.requireKnowledgeBaseRead(knowledgeBaseId);
        return knowledgeImportTaskRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    @Override
    @Transactional
    public KnowledgeImportTask cancelTask(Long taskId) {
        KnowledgeImportTask task = knowledgeAccessGuard.requireImportTaskEdit(taskId);
        if (task.getStatus() == KnowledgeImportTask.Status.SUCCESS
                || task.getStatus() == KnowledgeImportTask.Status.FAILED
                || task.getStatus() == KnowledgeImportTask.Status.CANCELLED) {
            return task;
        }
        knowledgeImportTaskRepository.requestCancel(
                taskId,
                Instant.now(),
                List.of(KnowledgeImportTask.Status.PENDING, KnowledgeImportTask.Status.RUNNING)
        );
        return loadTaskRequired(taskId);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void resumePendingImportTasksOnStartup() {
        List<KnowledgeImportTask> pendingTasks = knowledgeImportTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeImportTask.Status.PENDING);
        List<KnowledgeImportTask> runningTasks = knowledgeImportTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeImportTask.Status.RUNNING);
        Instant now = Instant.now();
        runningTasks.forEach(task -> knowledgeImportTaskRepository.resetStatus(
                task.getId(),
                KnowledgeImportTask.Status.RUNNING,
                KnowledgeImportTask.Status.PENDING,
                "Recovered stale RUNNING task after application restart",
                now
        ));
        if (pendingTasks.isEmpty() && runningTasks.isEmpty()) {
            return;
        }
        List<KnowledgeImportTask> recoverableTasks = knowledgeImportTaskRepository.findByStatusOrderByCreatedAtAsc(KnowledgeImportTask.Status.PENDING);
        log.info("Resubmitting {} recoverable knowledge import task(s) on startup", recoverableTasks.size());
        recoverableTasks.forEach(task -> submitImportTask(task.getId(), null));
    }

    private void submitImportTask(Long taskId, KnowledgeDocumentCreateRequest request) {
        if (taskId == null) {
            return;
        }
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    doSubmitImportTask(taskId, request);
                }
            });
            return;
        }
        doSubmitImportTask(taskId, request);
    }

    private void doSubmitImportTask(Long taskId, KnowledgeDocumentCreateRequest request) {
        try {
            aiKnowledgeTaskExecutor.execute(() -> runImportTask(taskId, request));
        } catch (RejectedExecutionException ex) {
            String error = trimError("Task queue is full: " + ex.getMessage());
            log.error("Failed to submit knowledge import task {}: {}", taskId, error, ex);
            failBeforeStart(taskId, error);
        }
    }

    private void runImportTask(Long taskId, KnowledgeDocumentCreateRequest request) {
        if (!startTaskIfPending(taskId)) {
            return;
        }
        processTask(taskId, request);
    }

    private KnowledgeImportTask loadTaskRequired(Long taskId) {
        return knowledgeImportTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("Knowledge import task not found"));
    }

    private void processTask(Long taskId, KnowledgeDocumentCreateRequest request) {
        KnowledgeImportTask task = loadTaskRequired(taskId);
        Path zipPath = Paths.get(task.getTempZipPath()).toAbsolutePath().normalize();
        Path taskDir = zipPath.getParent();
        String batchId = UUID.randomUUID().toString().replace("-", "");

        try {
            ensureNotCancelled(taskId);

            List<ZipEntry> importableEntries = new ArrayList<>();
            try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
                List<? extends ZipEntry> allEntries = zipFile.stream().toList();
                for (ZipEntry entry : allEntries) {
                    if (entry.isDirectory()) {
                        continue;
                    }
                    String entryName = normalizeZipEntryName(entry.getName());
                    if (!StringUtils.hasText(entryName) || shouldSkipProjectEntry(entryName)) {
                        continue;
                    }
                    if (!isImportableExtension(getExtension(entryName))) {
                        continue;
                    }
                    importableEntries.add(entry);
                    if (importableEntries.size() >= MAX_IMPORTABLE_FILES) {
                        break;
                    }
                }
            }

            task = loadTaskRequired(taskId);
            task.setCurrentStage(KnowledgeImportTask.Stage.SCANNING);
            task.setTotalFiles(importableEntries.size());
            task.setUpdatedAt(Instant.now());
            knowledgeImportTaskRepository.save(task);
            ensureNotCancelled(taskId);

            if (importableEntries.isEmpty()) {
                markSuccess(taskId, "No importable files found in ZIP archive");
                return;
            }

            Path extractDir = taskDir.resolve("extracted");
            Files.createDirectories(extractDir);

            int processed = 0;
            try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
                for (ZipEntry entry : importableEntries) {
                    ensureNotCancelled(taskId);
                    task = loadTaskRequired(taskId);

                    String entryName = normalizeZipEntryName(entry.getName());
                    task.setCurrentStage(KnowledgeImportTask.Stage.IMPORTING);
                    task.setCurrentFile(entryName);
                    task.setScannedFiles(processed);
                    task.setProgressPercent(calcProgress(processed, importableEntries.size()));
                    task.setUpdatedAt(Instant.now());
                    knowledgeImportTaskRepository.save(task);

                    Path extractedFile = safeExtractEntry(zipFile, entry, extractDir, entryName);
                    if (extractedFile == null) {
                        task.setSkippedFiles(nvl(task.getSkippedFiles()) + 1);
                        task.setUpdatedAt(Instant.now());
                        knowledgeImportTaskRepository.save(task);
                        processed++;
                        continue;
                    }

                    try {
                        importSingleExtractedFile(task.getKnowledgeBaseId(), extractedFile, entryName, task.getZipName(), batchId, request);
                        task = loadTaskRequired(taskId);
                        task.setImportedFiles(nvl(task.getImportedFiles()) + 1);
                    } catch (Exception ex) {
                        task = loadTaskRequired(taskId);
                        task.setFailedFiles(nvl(task.getFailedFiles()) + 1);
                        task.setErrorMessage(trimError(ex.getMessage()));
                    }

                    processed++;
                    task.setScannedFiles(processed);
                    task.setProgressPercent(calcProgress(processed, importableEntries.size()));
                    task.setUpdatedAt(Instant.now());
                    knowledgeImportTaskRepository.save(task);
                }
            }

            ensureNotCancelled(taskId);
            markSuccess(taskId, null);
        } catch (TaskCancelledException ex) {
            markCancelled(taskId);
        } catch (Exception ex) {
            markFailed(taskId, ex);
        } finally {
            cleanupTemp(taskDir);
        }
    }

    private KnowledgeDocument importSingleExtractedFile(Long knowledgeBaseId,
                                                        Path extractedPath,
                                                        String entryPath,
                                                        String zipName,
                                                        String batchId,
                                                        KnowledgeDocumentCreateRequest request) throws Exception {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found"));

        String originalFileName = sanitizeFileName(extractedPath.getFileName().toString());
        String mimeType = resolveMimeType(extractedPath, originalFileName, null);
        Path storedPath = storePathFile(knowledgeBaseId, extractedPath, originalFileName);
        String extractedText = extractTextFromFile(storedPath, originalFileName, mimeType);
        String normalizedText = normalizeContent(extractedText);

        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(request == null || request.getSourceType() == null ? KnowledgeDocument.SourceType.PROJECT_DOC : request.getSourceType());
        entity.setSourceRefId(request == null ? null : request.getSourceRefId());
        entity.setSourceUrl(entryPath);
        entity.setTitle(entryPath);
        entity.setFileName(originalFileName);
        entity.setArchiveName(zipName);
        entity.setArchiveEntryPath(entryPath);
        entity.setImportBatchId(batchId);
        entity.setMimeType(mimeType);
        entity.setStoragePath(toRelativeStoragePath(storedPath));
        entity.setLanguage(request == null ? null : trimToNull(request.getLanguage()));
        entity.setUploadedBy(request == null ? null : request.getUploadedBy());
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

        if (!StringUtils.hasText(normalizedText)) {
            entity.setContentText("");
            entity.setContentHash(null);
            entity.setStatus(KnowledgeDocument.Status.FAILED);
            entity.setErrorMessage("No readable content extracted from file");
            return knowledgeDocumentRepository.save(entity);
        }

        entity.setContentText(normalizedText);
        entity.setContentHash(sha256(normalizedText));
        entity.setStatus(KnowledgeDocument.Status.UPLOADED);
        entity.setErrorMessage(null);

        KnowledgeDocument saved = knowledgeDocumentRepository.save(entity);
        refreshKnowledgeBaseStats(knowledgeBaseId, null);
        reindexDocument(knowledgeBase, saved);
        return knowledgeDocumentRepository.findById(saved.getId()).orElse(saved);
    }

    private void reindexDocument(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        Instant now = Instant.now();
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setKnowledgeBase(knowledgeBase);
        task.setDocument(document);
        task.setTaskType(KnowledgeIndexTask.TaskType.REINDEX);
        task.setStatus(KnowledgeIndexTask.Status.PENDING);
        task.setRetryCount(0);
        task.setErrorMessage(null);
        task.setCreatedAt(now);
        task.setUpdatedAt(now);
        KnowledgeIndexTask savedTask = knowledgeIndexTaskRepository.save(task);
        if (!startIndexTaskIfPending(savedTask.getId())) {
            return;
        }
        try {
            parseDocument(document);
            RebuildChunksResult rebuildResult = rebuildChunks(knowledgeBase, document);
            ensureChunksBuilt(rebuildResult);
            rebuildCodeIndex(knowledgeBase, document, rebuildResult);
            knowledgeEmbeddingService.backfillDocumentEmbeddings(
                    document.getId(),
                    knowledgeBase.getEmbeddingProvider(),
                    knowledgeBase.getEmbeddingModel(),
                    null
            );
            markDocumentIndexed(document);
            markIndexTaskTerminal(savedTask.getId(), KnowledgeIndexTask.Status.SUCCESS, null);
            refreshKnowledgeBaseStats(knowledgeBase.getId(), Instant.now());
        } catch (Exception ex) {
            String error = trimError(ex.getMessage());
            markIndexTaskTerminal(savedTask.getId(), KnowledgeIndexTask.Status.FAILED, error);
            document.setStatus(KnowledgeDocument.Status.FAILED);
            document.setErrorMessage(error);
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
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
            throw new IllegalArgumentException("閺傚洦銆傞崘鍛啇娑撳秷鍏樻稉铏光敄");
        }
        String filePath = resolveDocumentPath(document);
        document.setTitle(StringUtils.hasText(document.getTitle()) ? document.getTitle() : filePath);
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
            document.setErrorMessage("Chunking produced no chunks");
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
            return new RebuildChunksResult(List.of(), draft);
        }

        List<KnowledgeChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < parts.size(); i++) {
            KnowledgeChunkingService.ChunkDraft part = parts.get(i);
            String content = normalizeContent(part.content());
            if (!StringUtils.hasText(content)) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeBase(knowledgeBase);
            chunk.setDocument(document);
            chunk.setChunkIndex(part.chunkIndex() == null ? i : part.chunkIndex());
            chunk.setChunkType(resolveChunkType(part.language()));
            chunk.setContent(content);
            chunk.setTokenCount(part.tokenCount() == null ? estimateTokens(content) : part.tokenCount());
            chunk.setEmbeddingProvider(trimToNull(knowledgeBase.getEmbeddingProvider()));
            chunk.setEmbeddingModel(trimToNull(knowledgeBase.getEmbeddingModel()));
            chunk.setVectorId(null);
            chunk.setFilePath(StringUtils.hasText(part.path()) ? part.path() : document.getFilePath());
            chunk.setStartLine(part.startLine());
            chunk.setStartColumn(null);
            chunk.setEndLine(part.endLine());
            chunk.setEndColumn(null);
            chunk.setSectionPath(part.sectionName());
            chunk.setContentHash(sha256(content));
            chunk.setMetadataJson(part.metadataJson());
            chunk.setCreatedAt(now);
            chunk.setUpdatedAt(now);
            chunks.add(chunk);
        }

        List<KnowledgeChunk> savedChunks = List.of();
        if (!chunks.isEmpty()) {
            savedChunks = knowledgeChunkRepository.saveAll(chunks);
            knowledgeChunkRepository.flush();
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
        document.setErrorMessage(null);
        document.setIndexedAt(Instant.now());
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private void refreshKnowledgeBaseStats(Long knowledgeBaseId, Instant indexedAt) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("Knowledge base not found"));
        int docCount = Math.toIntExact(knowledgeDocumentRepository.countByKnowledgeBase_Id(knowledgeBaseId));
        int chunkCount = Math.toIntExact(knowledgeChunkRepository.countByKnowledgeBase_Id(knowledgeBaseId));
        knowledgeBase.setDocCount(docCount);
        knowledgeBase.setChunkCount(chunkCount);
        if (chunkCount > 0) {
            knowledgeBase.setStatus(KnowledgeBase.Status.ACTIVE);
            knowledgeBase.setLastIndexedAt(indexedAt == null ? knowledgeBase.getLastIndexedAt() : indexedAt);
        } else if (docCount > 0) {
            knowledgeBase.setStatus(KnowledgeBase.Status.DRAFT);
        } else {
            knowledgeBase.setStatus(KnowledgeBase.Status.DRAFT);
            knowledgeBase.setLastIndexedAt(null);
        }
        knowledgeBase.setUpdatedAt(Instant.now());
        knowledgeBaseRepository.save(knowledgeBase);
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

    private Path ensureImportDirectory() throws IOException {
        Path baseDir = Paths.get(importRoot).toAbsolutePath().normalize();
        Files.createDirectories(baseDir);
        return baseDir;
    }

    private String toRelativeStoragePath(Path storedPath) throws IOException {
        Path baseDir = ensureStorageDirectory();
        return baseDir.relativize(storedPath.toAbsolutePath().normalize()).toString().replace('\\', '/');
    }

    private String extractTextFromFile(Path storedPath, String originalFileName, String mimeType) throws IOException {
        String ext = getExtension(originalFileName);
        return switch (ext) {
            case "pdf" -> extractPdfText(storedPath);
            case "docx" -> extractDocxText(storedPath);
            case "doc" -> extractDocText(storedPath);
            default -> extractPlainText(storedPath, mimeType, ext);
        };
    }

    private String extractPdfText(Path path) throws IOException {
        try (PDDocument document = Loader.loadPDF(path.toFile())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocxText(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             XWPFDocument document = new XWPFDocument(inputStream);
             XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractDocText(Path path) throws IOException {
        try (InputStream inputStream = Files.newInputStream(path);
             HWPFDocument document = new HWPFDocument(inputStream);
             WordExtractor extractor = new WordExtractor(document)) {
            return extractor.getText();
        }
    }

    private String extractPlainText(Path path, String mimeType, String ext) throws IOException {
        if (isSupportedTextExtension(ext) || (mimeType != null && mimeType.startsWith("text/"))) {
            byte[] bytes = Files.readAllBytes(path);
            return stripUtf8Bom(new String(bytes, StandardCharsets.UTF_8));
        }
        throw new IllegalArgumentException("閺嗗倷绗夐弨顖涘瘮鐟欙絾鐎界拠銉︽瀮娴犲墎琚崹? " + ext);
    }

    private Path safeExtractEntry(ZipFile zipFile, ZipEntry entry, Path extractRoot, String entryName) throws IOException {
        Path target = extractRoot.resolve(entryName).normalize();
        if (!target.startsWith(extractRoot)) {
            return null;
        }
        Files.createDirectories(target.getParent());
        try (InputStream inputStream = zipFile.getInputStream(entry);
             OutputStream outputStream = Files.newOutputStream(target)) {
            byte[] buffer = new byte[8192];
            long total = 0L;
            int len;
            while ((len = inputStream.read(buffer)) > 0) {
                total += len;
                if (total > MAX_ENTRY_BYTES) {
                    return null;
                }
                outputStream.write(buffer, 0, len);
            }
        }
        return target;
    }

    private boolean shouldSkipProjectEntry(String entryPath) {
        String s = entryPath.toLowerCase();
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
                || s.contains("/_cacache/")
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

    private boolean isImportableExtension(String ext) {
        return Set.of(
                "txt", "md", "markdown", "json", "csv", "js", "ts", "java", "xml", "html", "htm",
                "css", "vue", "sql", "yml", "yaml", "properties", "log", "text", "pdf", "doc", "docx"
        ).contains(ext);
    }

    private boolean isSupportedTextExtension(String ext) {
        return Set.of(
                "txt", "md", "markdown", "json", "csv", "js", "ts", "java", "xml", "html", "htm",
                "css", "vue", "sql", "yml", "yaml", "properties", "log", "text"
        ).contains(ext);
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
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase();
    }

    private String stripUtf8Bom(String text) {
        if (text != null && !text.isEmpty() && text.charAt(0) == '\ufeff') {
            return text.substring(1);
        }
        return text;
    }

    private String normalizeContent(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = stripUtf8Bom(text)
                .replace("\r\n", "\n")
                .replace("\r", "\n");
        normalized = normalized.replaceAll("\u0000", "");
        normalized = normalized.replaceAll("[\\t\\u000B\\f]+", " ");
        normalized = normalized.replaceAll("(?m)[ \\t]+$", "");
        normalized = normalized.replaceAll("\n{3,}", "\n\n").trim();
        return normalized;
    }

    private String resolveMimeType(Path path, String fileName, String fallback) {
        try {
            String detected = path != null ? Files.probeContentType(path) : null;
            if (StringUtils.hasText(detected)) {
                return detected;
            }
        } catch (IOException ignored) {
        }
        String guessed = URLConnection.guessContentTypeFromName(fileName);
        if (StringUtils.hasText(guessed)) {
            return guessed;
        }
        return StringUtils.hasText(fallback) ? fallback : "application/octet-stream";
    }

    private int estimateTokens(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    private String trimToNull(String text) {
        if (!StringUtils.hasText(text)) {
            return null;
        }
        return text.trim();
    }

    private String trimError(String message) {
        if (!StringUtils.hasText(message)) {
            return "閺堫亞鐓￠柨娆掝嚖";
        }
        return message.length() > 500 ? message.substring(0, 500) : message;
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(Character.forDigit((b >> 4) & 0xF, 16));
                sb.append(Character.forDigit(b & 0xF, 16));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 is not available", e);
        }
    }

    private boolean startTaskIfPending(Long taskId) {
        Instant now = Instant.now();
        int updated = knowledgeImportTaskRepository.transitionStatusToRunning(
                taskId,
                KnowledgeImportTask.Status.PENDING,
                KnowledgeImportTask.Status.RUNNING,
                KnowledgeImportTask.Stage.SCANNING,
                0,
                null,
                now
        );
        if (updated > 0) {
            return true;
        }
        KnowledgeImportTask current = knowledgeImportTaskRepository.findById(taskId).orElse(null);
        if (current != null) {
            log.info("Skip knowledge import task {} because current status is {}", taskId, current.getStatus());
        }
        return false;
    }

    private void failBeforeStart(Long taskId, String errorMessage) {
        Instant now = Instant.now();
        int started = knowledgeImportTaskRepository.transitionStatusToRunning(
                taskId,
                KnowledgeImportTask.Status.PENDING,
                KnowledgeImportTask.Status.RUNNING,
                KnowledgeImportTask.Stage.SCANNING,
                0,
                null,
                now
        );
        if (started > 0) {
            markTerminal(taskId, KnowledgeImportTask.Status.FAILED, KnowledgeImportTask.Stage.FINISHED, 0, null, errorMessage);
        }
    }

    private void ensureNotCancelled(Long taskId) {
        if (isCancellationRequested(taskId)) {
            throw new TaskCancelledException();
        }
    }

    private boolean isCancellationRequested(Long taskId) {
        KnowledgeImportTask task = loadTaskRequired(taskId);
        return Boolean.TRUE.equals(task.getCancelRequested());
    }

    private void markSuccess(Long taskId, String message) {
        markTerminal(
                taskId,
                KnowledgeImportTask.Status.SUCCESS,
                KnowledgeImportTask.Stage.FINISHED,
                100,
                null,
                message
        );
    }

    private void markFailed(Long taskId, Exception ex) {
        KnowledgeImportTask task = loadTaskRequired(taskId);
        markTerminal(
                taskId,
                KnowledgeImportTask.Status.FAILED,
                KnowledgeImportTask.Stage.FINISHED,
                nvl(task.getProgressPercent()),
                null,
                trimError(ex.getMessage())
        );
    }

    private void markCancelled(Long taskId) {
        KnowledgeImportTask task = loadTaskRequired(taskId);
        markTerminal(
                taskId,
                KnowledgeImportTask.Status.CANCELLED,
                KnowledgeImportTask.Stage.CANCELLED,
                nvl(task.getProgressPercent()),
                null,
                "Cancellation requested"
        );
    }

    private void markTerminal(Long taskId,
                              KnowledgeImportTask.Status status,
                              KnowledgeImportTask.Stage stage,
                              int progress,
                              String currentFile,
                              String errorMessage) {
        Instant now = Instant.now();
        int updated = knowledgeImportTaskRepository.transitionStatusFromRunning(
                taskId,
                KnowledgeImportTask.Status.RUNNING,
                status,
                stage,
                progress,
                currentFile,
                trimToNull(errorMessage),
                now,
                now
        );
        if (updated == 0) {
            KnowledgeImportTask current = knowledgeImportTaskRepository.findById(taskId).orElse(null);
            if (current != null) {
                log.info("Skip terminal transition for import task {} because current status is {}", taskId, current.getStatus());
            }
        }
    }

    private boolean startIndexTaskIfPending(Long taskId) {
        Instant now = Instant.now();
        int updated = knowledgeIndexTaskRepository.transitionStatusToRunning(
                taskId,
                KnowledgeIndexTask.Status.PENDING,
                KnowledgeIndexTask.Status.RUNNING,
                now,
                now
        );
        return updated > 0;
    }

    private void markIndexTaskTerminal(Long taskId, KnowledgeIndexTask.Status status, String errorMessage) {
        Instant now = Instant.now();
        knowledgeIndexTaskRepository.transitionStatusFromRunning(
                taskId,
                KnowledgeIndexTask.Status.RUNNING,
                status,
                trimToNull(errorMessage),
                now,
                now
        );
    }

    private static final class TaskCancelledException extends RuntimeException {
    }

    private void cleanupTemp(Path taskDir) {
        if (taskDir == null) {
            return;
        }
        try {
            FileSystemUtils.deleteRecursively(taskDir);
        } catch (IOException ignored) {
        }
    }

    private int calcProgress(int done, int total) {
        if (total <= 0) {
            return 0;
        }
        int value = (int) Math.floor((done * 100.0) / total);
        return Math.max(0, Math.min(value, 99));
    }

    private int nvl(Integer value) {
        return value == null ? 0 : value;
    }

    private KnowledgeDocumentCreateRequest cloneRequest(KnowledgeDocumentCreateRequest request) {
        KnowledgeDocumentCreateRequest cloned = new KnowledgeDocumentCreateRequest();
        if (request == null) {
            cloned.setSourceType(KnowledgeDocument.SourceType.PROJECT_DOC);
            cloned.setAutoIndex(Boolean.TRUE);
            return cloned;
        }
        cloned.setSourceType(request.getSourceType() == null ? KnowledgeDocument.SourceType.PROJECT_DOC : request.getSourceType());
        cloned.setSourceRefId(request.getSourceRefId());
        cloned.setSourceUrl(request.getSourceUrl());
        cloned.setTitle(request.getTitle());
        cloned.setFileName(request.getFileName());
        cloned.setMimeType(request.getMimeType());
        cloned.setContentText(request.getContentText());
        cloned.setContentHash(request.getContentHash());
        cloned.setLanguage(request.getLanguage());
        cloned.setUploadedBy(request.getUploadedBy());
        cloned.setAutoIndex(Boolean.TRUE);
        return cloned;
    }

    private record RebuildChunksResult(
            List<KnowledgeChunk> chunks,
            KnowledgeChunkingService.IndexBuildResult indexDraft
    ) {
    }
}


