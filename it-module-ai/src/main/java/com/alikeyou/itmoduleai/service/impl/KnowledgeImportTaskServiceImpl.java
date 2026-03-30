package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.entity.KnowledgeIndexTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.KnowledgeImportTaskService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.springframework.util.FileSystemUtils;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

@Service
@RequiredArgsConstructor
public class KnowledgeImportTaskServiceImpl implements KnowledgeImportTaskService {

    private static final int FIXED_CHUNK_SIZE = 800;
    private static final int FIXED_CHUNK_OVERLAP = 120;
    private static final int BLOCK_CHUNK_MAX_LENGTH = 900;
    private static final long MAX_ENTRY_BYTES = 10L * 1024 * 1024;
    private static final int MAX_IMPORTABLE_FILES = 1500;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private final KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.knowledge-document.storage-root:${user.dir}/.runtime/knowledge-documents}")
    private String storageRoot;

    @Value("${app.ai.knowledge-import.storage-root:${user.dir}/.runtime/knowledge-imports}")
    private String importRoot;

    @Override
    @Transactional
    public KnowledgeImportTask createZipImportTask(Long knowledgeBaseId, MultipartFile file, KnowledgeDocumentCreateRequest request) {
        KnowledgeBase knowledgeBase = knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP 文件不能为空");
        }

        String zipName = sanitizeFileName(file.getOriginalFilename());
        if (!zipName.toLowerCase().endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 .zip 文件");
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
            task.setCreatedAt(Instant.now());
            task.setUpdatedAt(Instant.now());

            KnowledgeImportTask saved = knowledgeImportTaskRepository.save(task);
            KnowledgeDocumentCreateRequest safeRequest = cloneRequest(request);
            CompletableFuture.runAsync(() -> processTask(saved.getId(), safeRequest));
            return saved;
        } catch (IOException ex) {
            throw new RuntimeException("保存 ZIP 文件失败: " + trimError(ex.getMessage()), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeImportTask getTask(Long taskId) {
        return knowledgeImportTaskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("导入任务不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeImportTask> listByKnowledgeBase(Long knowledgeBaseId) {
        return knowledgeImportTaskRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    @Override
    @Transactional
    public KnowledgeImportTask cancelTask(Long taskId) {
        KnowledgeImportTask task = getTask(taskId);
        if (task.getStatus() == KnowledgeImportTask.Status.SUCCESS
                || task.getStatus() == KnowledgeImportTask.Status.FAILED
                || task.getStatus() == KnowledgeImportTask.Status.CANCELLED) {
            return task;
        }
        task.setCancelRequested(Boolean.TRUE);
        task.setUpdatedAt(Instant.now());
        return knowledgeImportTaskRepository.save(task);
    }

    private void processTask(Long taskId, KnowledgeDocumentCreateRequest request) {
        KnowledgeImportTask task = getTask(taskId);
        Path zipPath = Paths.get(task.getTempZipPath()).toAbsolutePath().normalize();
        Path taskDir = zipPath.getParent();
        String batchId = UUID.randomUUID().toString().replace("-", "");

        try {
            markRunning(task, KnowledgeImportTask.Stage.SCANNING, 0, null);

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

            task = getTask(taskId);
            task.setTotalFiles(importableEntries.size());
            task.setUpdatedAt(Instant.now());
            knowledgeImportTaskRepository.save(task);

            if (importableEntries.isEmpty()) {
                markSuccess(taskId, "ZIP 中没有可导入的源码/文档文件");
                cleanupTemp(taskDir);
                return;
            }

            Path extractDir = taskDir.resolve("extracted");
            Files.createDirectories(extractDir);

            int processed = 0;
            try (ZipFile zipFile = new ZipFile(zipPath.toFile(), StandardCharsets.UTF_8)) {
                for (ZipEntry entry : importableEntries) {
                    task = getTask(taskId);
                    if (Boolean.TRUE.equals(task.getCancelRequested())) {
                        markCancelled(taskId);
                        cleanupTemp(taskDir);
                        return;
                    }

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
                        importSingleExtractedFile(task.getKnowledgeBase().getId(), extractedFile, entryName, task.getZipName(), batchId, request);
                        task = getTask(taskId);
                        task.setImportedFiles(nvl(task.getImportedFiles()) + 1);
                    } catch (Exception ex) {
                        task = getTask(taskId);
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

            if (Boolean.TRUE.equals(getTask(taskId).getCancelRequested())) {
                markCancelled(taskId);
            } else {
                markSuccess(taskId, null);
            }
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
                .orElseThrow(() -> new RuntimeException("知识库不存在"));

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
            entity.setErrorMessage("未能从文件中提取到正文内容");
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
        KnowledgeIndexTask task = new KnowledgeIndexTask();
        task.setKnowledgeBase(knowledgeBase);
        task.setDocument(document);
        task.setTaskType(KnowledgeIndexTask.TaskType.REINDEX);
        task.setStatus(KnowledgeIndexTask.Status.RUNNING);
        task.setRetryCount(0);
        task.setCreatedAt(Instant.now());
        task.setStartedAt(Instant.now());
        KnowledgeIndexTask savedTask = knowledgeIndexTaskRepository.save(task);
        try {
            parseDocument(document);
            rebuildChunks(knowledgeBase, document);
            refreshChunkEmbeddings(knowledgeBase, document);
            markDocumentIndexed(document);
            savedTask.setStatus(KnowledgeIndexTask.Status.SUCCESS);
            savedTask.setErrorMessage(null);
            savedTask.setFinishedAt(Instant.now());
            knowledgeIndexTaskRepository.save(savedTask);
            refreshKnowledgeBaseStats(knowledgeBase.getId(), Instant.now());
        } catch (Exception ex) {
            savedTask.setStatus(KnowledgeIndexTask.Status.FAILED);
            savedTask.setErrorMessage(trimError(ex.getMessage()));
            savedTask.setFinishedAt(Instant.now());
            knowledgeIndexTaskRepository.save(savedTask);

            document.setStatus(KnowledgeDocument.Status.FAILED);
            document.setErrorMessage(trimError(ex.getMessage()));
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
        }
    }

    private void parseDocument(KnowledgeDocument document) {
        String content = normalizeContent(document.getContentText());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
        document.setContentText(content);
        document.setContentHash(StringUtils.hasText(document.getContentHash()) ? document.getContentHash().trim() : sha256(content));
        document.setErrorMessage(null);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private void rebuildChunks(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        knowledgeChunkRepository.deleteByDocumentId(document.getId());
        knowledgeChunkRepository.flush();

        List<String> contents = splitIntoChunks(document.getContentText(), knowledgeBase.getChunkStrategy());
        List<KnowledgeChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();

        for (int i = 0; i < contents.size(); i++) {
            String content = contents.get(i);
            if (!StringUtils.hasText(content)) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeBase(knowledgeBase);
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setContent(content);
            chunk.setTokenCount(estimateTokens(content));
            chunk.setEmbeddingProvider(trimToNull(knowledgeBase.getEmbeddingProvider()));
            chunk.setEmbeddingModel(trimToNull(knowledgeBase.getEmbeddingModel()));
            chunk.setVectorId(null);
            chunk.setMetadataJson(buildChunkMetadata(knowledgeBase, document, i, content));
            chunk.setCreatedAt(now);
            chunks.add(chunk);
        }

        if (!chunks.isEmpty()) {
            knowledgeChunkRepository.saveAll(chunks);
            knowledgeChunkRepository.flush();
            markDocumentIndexed(document);
        } else {
            document.setStatus(KnowledgeDocument.Status.UPLOADED);
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
        }
    }

    private void refreshChunkEmbeddings(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(document.getId());
        if (chunks.isEmpty()) {
            return;
        }
        Instant now = Instant.now();
        for (KnowledgeChunk chunk : chunks) {
            chunk.setEmbeddingProvider(trimToNull(knowledgeBase.getEmbeddingProvider()));
            chunk.setEmbeddingModel(trimToNull(knowledgeBase.getEmbeddingModel()));
            chunk.setVectorId(null);
            chunk.setMetadataJson(buildChunkMetadata(knowledgeBase, document, chunk.getChunkIndex(), chunk.getContent()));
            chunk.setCreatedAt(chunk.getCreatedAt() == null ? now : chunk.getCreatedAt());
        }
        knowledgeChunkRepository.saveAll(chunks);
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
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
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
        throw new IllegalArgumentException("暂不支持解析该文件类型: " + ext);
    }

    private List<String> splitIntoChunks(String content, KnowledgeBase.ChunkStrategy strategy) {
        String normalized = normalizeContent(content);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }
        KnowledgeBase.ChunkStrategy actualStrategy = strategy == null ? KnowledgeBase.ChunkStrategy.PARAGRAPH : strategy;
        return switch (actualStrategy) {
            case FIXED, CUSTOM -> splitFixed(normalized, FIXED_CHUNK_SIZE, FIXED_CHUNK_OVERLAP);
            case PARAGRAPH -> splitByBlocks(normalized);
            case MARKDOWN -> splitMarkdown(normalized);
        };
    }

    private List<String> splitByBlocks(String content) {
        String[] rawBlocks = content.split("\\n\\s*\\n+");
        List<String> blocks = new ArrayList<>();
        for (String raw : rawBlocks) {
            String item = raw == null ? null : raw.trim();
            if (StringUtils.hasText(item)) {
                blocks.add(item);
            }
        }
        return mergeBlocks(blocks);
    }

    private List<String> splitMarkdown(String content) {
        String normalized = content.replaceAll("(?m)(^#{1,6}\\s+)", "\\n\\n$1");
        return splitByBlocks(normalized);
    }

    private List<String> mergeBlocks(List<String> blocks) {
        if (blocks.isEmpty()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        for (String block : blocks) {
            if (block.length() > BLOCK_CHUNK_MAX_LENGTH) {
                if (current.length() > 0) {
                    result.add(current.toString().trim());
                    current.setLength(0);
                }
                result.addAll(splitFixed(block, FIXED_CHUNK_SIZE, FIXED_CHUNK_OVERLAP));
                continue;
            }
            if (current.length() == 0) {
                current.append(block);
                continue;
            }
            if (current.length() + 2 + block.length() <= BLOCK_CHUNK_MAX_LENGTH) {
                current.append("\n\n").append(block);
            } else {
                result.add(current.toString().trim());
                current.setLength(0);
                current.append(block);
            }
        }
        if (current.length() > 0) {
            result.add(current.toString().trim());
        }
        return result;
    }

    private List<String> splitFixed(String content, int maxLength, int overlap) {
        List<String> result = new ArrayList<>();
        if (!StringUtils.hasText(content)) {
            return result;
        }
        int safeMaxLength = Math.max(200, maxLength);
        int safeOverlap = Math.max(0, Math.min(overlap, safeMaxLength - 50));
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + safeMaxLength);
            String part = content.substring(start, end).trim();
            if (StringUtils.hasText(part)) {
                result.add(part);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(start + 1, end - safeOverlap);
        }
        return result;
    }

    private String buildChunkMetadata(KnowledgeBase knowledgeBase, KnowledgeDocument document, Integer chunkIndex, String content) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("knowledgeBaseId", knowledgeBase.getId());
        metadata.put("knowledgeBaseName", knowledgeBase.getName());
        metadata.put("documentId", document.getId());
        metadata.put("documentTitle", document.getTitle());
        metadata.put("sourceType", document.getSourceType() == null ? null : document.getSourceType().name());
        metadata.put("sourceRefId", document.getSourceRefId());
        metadata.put("chunkIndex", chunkIndex);
        metadata.put("tokenCount", estimateTokens(content));
        metadata.put("embeddingProvider", trimToNull(knowledgeBase.getEmbeddingProvider()));
        metadata.put("embeddingModel", trimToNull(knowledgeBase.getEmbeddingModel()));
        metadata.put("archiveName", document.getArchiveName());
        metadata.put("archiveEntryPath", document.getArchiveEntryPath());
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"documentId\":" + document.getId() + ",\"chunkIndex\":" + chunkIndex + "}";
        }
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
            return "未知错误";
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
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    private void markRunning(KnowledgeImportTask task, KnowledgeImportTask.Stage stage, int progress, String currentFile) {
        task.setStatus(KnowledgeImportTask.Status.RUNNING);
        task.setCurrentStage(stage);
        task.setProgressPercent(progress);
        task.setCurrentFile(currentFile);
        task.setUpdatedAt(Instant.now());
        knowledgeImportTaskRepository.save(task);
    }

    private void markSuccess(Long taskId, String message) {
        KnowledgeImportTask task = getTask(taskId);
        task.setStatus(KnowledgeImportTask.Status.SUCCESS);
        task.setCurrentStage(KnowledgeImportTask.Stage.FINISHED);
        task.setProgressPercent(100);
        task.setCurrentFile(null);
        if (StringUtils.hasText(message)) {
            task.setErrorMessage(message);
        }
        task.setUpdatedAt(Instant.now());
        task.setFinishedAt(Instant.now());
        knowledgeImportTaskRepository.save(task);
    }

    private void markFailed(Long taskId, Exception ex) {
        KnowledgeImportTask task = getTask(taskId);
        task.setStatus(KnowledgeImportTask.Status.FAILED);
        task.setCurrentStage(KnowledgeImportTask.Stage.FINISHED);
        task.setErrorMessage(trimError(ex.getMessage()));
        task.setUpdatedAt(Instant.now());
        task.setFinishedAt(Instant.now());
        knowledgeImportTaskRepository.save(task);
    }

    private void markCancelled(Long taskId) {
        KnowledgeImportTask task = getTask(taskId);
        task.setStatus(KnowledgeImportTask.Status.CANCELLED);
        task.setCurrentStage(KnowledgeImportTask.Stage.CANCELLED);
        task.setErrorMessage("用户取消导入");
        task.setUpdatedAt(Instant.now());
        task.setFinishedAt(Instant.now());
        knowledgeImportTaskRepository.save(task);
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
}
