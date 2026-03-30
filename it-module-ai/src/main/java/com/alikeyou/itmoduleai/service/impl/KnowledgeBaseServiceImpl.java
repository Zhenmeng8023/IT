package com.alikeyou.itmoduleai.service.impl;

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
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeIndexTaskRepository;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeBaseServiceImpl implements KnowledgeBaseService {

    private static final int DEFAULT_TOP_K = 5;
    private static final int FIXED_CHUNK_SIZE = 800;
    private static final int FIXED_CHUNK_OVERLAP = 120;
    private static final int BLOCK_CHUNK_MAX_LENGTH = 900;

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeIndexTaskRepository knowledgeIndexTaskRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.ai.knowledge-document.storage-root:${user.dir}/.runtime/knowledge-documents}")
    private String storageRoot;

    @Override
    public KnowledgeBase createKnowledgeBase(KnowledgeBaseCreateRequest request) {
        validateKnowledgeBaseRequest(request);

        Instant now = Instant.now();
        KnowledgeBase entity = new KnowledgeBase();
        entity.setScopeType(request.getScopeType() == null ? KnowledgeBase.ScopeType.PERSONAL : request.getScopeType());
        entity.setProjectId(request.getProjectId());
        entity.setOwnerId(request.getOwnerId());
        entity.setName(trimToNull(request.getName()));
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setSourceType(request.getSourceType() == null ? KnowledgeBase.SourceType.MANUAL : request.getSourceType());
        entity.setChunkStrategy(request.getChunkStrategy() == null ? KnowledgeBase.ChunkStrategy.PARAGRAPH : request.getChunkStrategy());
        entity.setDefaultTopK(request.getDefaultTopK() == null ? DEFAULT_TOP_K : request.getDefaultTopK());
        entity.setVisibility(request.getVisibility() == null ? KnowledgeBase.Visibility.PRIVATE : request.getVisibility());
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
            throw new IllegalArgumentException("知识库请求不能为空");
        }

        KnowledgeBase entity = getById(id);
        if (request.getScopeType() != null) entity.setScopeType(request.getScopeType());
        if (request.getProjectId() != null) entity.setProjectId(request.getProjectId());
        if (request.getOwnerId() != null) entity.setOwnerId(request.getOwnerId());
        if (StringUtils.hasText(request.getName())) entity.setName(trimToNull(request.getName()));
        if (request.getDescription() != null) entity.setDescription(trimToNull(request.getDescription()));
        if (request.getSourceType() != null) entity.setSourceType(request.getSourceType());
        if (request.getChunkStrategy() != null) entity.setChunkStrategy(request.getChunkStrategy());
        if (request.getDefaultTopK() != null) entity.setDefaultTopK(request.getDefaultTopK());
        if (request.getVisibility() != null) entity.setVisibility(request.getVisibility());
        entity.setUpdatedAt(Instant.now());

        KnowledgeBase saved = knowledgeBaseRepository.save(entity);
        ensureOwnerMember(saved);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeBase getById(Long id) {
        return knowledgeBaseRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("知识库不存在"));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByOwner(Long ownerId, Pageable pageable) {
        return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeBase> pageByProject(Long projectId, Pageable pageable) {
        return knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
    }

    @Override
    public KnowledgeDocument addDocument(Long knowledgeBaseId, KnowledgeDocumentCreateRequest request) {
        validateDocumentRequest(request);

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
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
            throw new IllegalArgumentException("files不能为空");
        }

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
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
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("ZIP 文件不能为空");
        }

        String zipName = sanitizeFileName(file.getOriginalFilename());
        if (!zipName.toLowerCase(Locale.ROOT).endsWith(".zip")) {
            throw new IllegalArgumentException("只支持 .zip 文件");
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
                        throw new RuntimeException("非法 ZIP 路径: " + entryName);
                    }

                    if (extractedPath.getParent() != null) {
                        Files.createDirectories(extractedPath.getParent());
                    }

                    long written = copyZipEntry(zis, extractedPath, maxEntryBytes);
                    totalBytes += written;
                    fileCount++;

                    if (totalBytes > maxTotalBytes) {
                        throw new RuntimeException("ZIP 解压后总体积过大");
                    }
                    if (fileCount > maxFileCount) {
                        throw new RuntimeException("ZIP 内文件数量过多");
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
            throw new RuntimeException("ZIP 导入失败: " + trimError(ex.getMessage()), ex);
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
        return knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByUpdatedAtDesc(knowledgeBaseId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeChunk> listChunks(Long documentId) {
        return knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(documentId);
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentBinary downloadDocument(Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("知识文档不存在"));

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
            throw new RuntimeException("下载文件失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeDocumentBinary downloadDocumentsZip(Long knowledgeBaseId, List<Long> documentIds) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        List<KnowledgeDocument> documents = loadDownloadDocuments(knowledgeBaseId, documentIds);
        if (documents.isEmpty()) {
            throw new RuntimeException("没有可下载的文档");
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
            throw new RuntimeException("打包下载失败: " + ex.getMessage(), ex);
        }
    }

    @Override
    public KnowledgeBaseMember addMember(Long knowledgeBaseId, KnowledgeBaseMemberCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new IllegalArgumentException("userId不能为空");
        }

        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        if (knowledgeBaseMemberRepository.existsByKnowledgeBase_IdAndUserId(knowledgeBaseId, request.getUserId())) {
            throw new RuntimeException("成员已存在");
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
        KnowledgeBaseMember entity = knowledgeBaseMemberRepository.findById(memberId)
                .orElseThrow(() -> new RuntimeException("知识库成员不存在"));
        if (!entity.getKnowledgeBase().getId().equals(knowledgeBaseId)) {
            throw new RuntimeException("成员不属于当前知识库");
        }
        knowledgeBaseMemberRepository.delete(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeBaseMember> listMembers(Long knowledgeBaseId) {
        return knowledgeBaseMemberRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);
    }

    @Override
    public KnowledgeIndexTask createIndexTask(Long knowledgeBaseId, KnowledgeIndexTaskCreateRequest request) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);

        KnowledgeIndexTask entity = new KnowledgeIndexTask();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setTaskType(resolveTaskType(request));
        entity.setStatus(KnowledgeIndexTask.Status.PENDING);
        entity.setRetryCount(0);
        entity.setCreatedAt(Instant.now());

        if (request != null && request.getDocumentId() != null) {
            entity.setDocument(loadDocument(knowledgeBaseId, request.getDocumentId()));
        }

        return executeTask(entity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexTask> listKnowledgeBaseTasks(Long knowledgeBaseId) {
        return knowledgeIndexTaskRepository.findByKnowledgeBase_IdOrderByCreatedAtDesc(knowledgeBaseId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<KnowledgeIndexTask> listDocumentTasks(Long documentId) {
        return knowledgeIndexTaskRepository.findByDocument_IdOrderByCreatedAtDesc(documentId);
    }

    private KnowledgeIndexTask executeTask(KnowledgeIndexTask task) {
        task.setStatus(KnowledgeIndexTask.Status.RUNNING);
        task.setStartedAt(Instant.now());
        if (task.getCreatedAt() == null) {
            task.setCreatedAt(Instant.now());
        }

        KnowledgeIndexTask savedTask = knowledgeIndexTaskRepository.save(task);
        try {
            runTask(savedTask);
            savedTask.setStatus(KnowledgeIndexTask.Status.SUCCESS);
            savedTask.setErrorMessage(null);
        } catch (Exception ex) {
            savedTask.setStatus(KnowledgeIndexTask.Status.FAILED);
            savedTask.setErrorMessage(trimError(ex.getMessage()));
        }
        savedTask.setFinishedAt(Instant.now());
        return knowledgeIndexTaskRepository.save(savedTask);
    }

    private void runTask(KnowledgeIndexTask task) {
        KnowledgeBase knowledgeBase = task.getKnowledgeBase();
        if (knowledgeBase == null) {
            throw new IllegalArgumentException("知识库不存在");
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
                rebuildChunks(knowledgeBase, managedDocument);
            }
            case EMBED -> {
                parseDocument(managedDocument);
                if (knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(managedDocument.getId()).isEmpty()) {
                    rebuildChunks(knowledgeBase, managedDocument);
                }
                refreshChunkEmbeddings(knowledgeBase, managedDocument);
                markDocumentIndexed(managedDocument);
            }
            case REINDEX -> {
                parseDocument(managedDocument);
                rebuildChunks(knowledgeBase, managedDocument);
                refreshChunkEmbeddings(knowledgeBase, managedDocument);
                markDocumentIndexed(managedDocument);
            }
        }
    }

    private void parseDocument(KnowledgeDocument document) {
        String content = normalizeContent(document.getContentText());
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("文档内容不能为空");
        }
        document.setTitle(resolveDocumentTitle(document.getTitle(), document.getFileName(), content));
        document.setContentText(content);
        document.setContentHash(StringUtils.hasText(document.getContentHash()) ? document.getContentHash().trim() : sha256(content));
        document.setErrorMessage(null);
        document.setStatus(KnowledgeDocument.Status.UPLOADED);
        document.setUpdatedAt(Instant.now());
        knowledgeDocumentRepository.save(document);
    }

    private void rebuildChunks(KnowledgeBase knowledgeBase, KnowledgeDocument document) {
        knowledgeChunkRepository.deleteByDocumentId(document.getId());

        List<String> texts = splitIntoChunks(document.getContentText(), knowledgeBase.getChunkStrategy());
        if (texts.isEmpty()) {
            document.setStatus(KnowledgeDocument.Status.FAILED);
            document.setErrorMessage("切片结果为空");
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
            return;
        }

        List<KnowledgeChunk> chunks = new ArrayList<>();
        Instant now = Instant.now();
        for (int i = 0; i < texts.size(); i++) {
            String text = normalizeContent(texts.get(i));
            if (!StringUtils.hasText(text)) {
                continue;
            }
            KnowledgeChunk chunk = new KnowledgeChunk();
            chunk.setKnowledgeBase(knowledgeBase);
            chunk.setDocument(document);
            chunk.setChunkIndex(i);
            chunk.setContent(text);
            chunk.setTokenCount(estimateTokens(text));
            chunk.setMetadataJson(buildChunkMetadata(document, text, i));
            chunk.setCreatedAt(now);
            chunks.add(chunk);
        }

        if (!chunks.isEmpty()) {
            knowledgeChunkRepository.saveAll(chunks);
        }

        if (knowledgeBase.getSourceType() == KnowledgeBase.SourceType.PROJECT_DOC || document.getSourceType() == KnowledgeDocument.SourceType.PROJECT_DOC) {
            document.setStatus(KnowledgeDocument.Status.UPLOADED);
        } else {
            markDocumentIndexed(document);
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
            if (!StringUtils.hasText(chunk.getVectorId())) {
                chunk.setVectorId(UUID.randomUUID().toString().replace("-", ""));
            }
            if (chunk.getCreatedAt() == null) {
                chunk.setCreatedAt(now);
            }
        }
        knowledgeChunkRepository.saveAll(chunks);

        if (!chunks.isEmpty()) {
            markDocumentIndexed(document);
        } else {
            document.setStatus(KnowledgeDocument.Status.UPLOADED);
            document.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(document);
        }
    }

    private void markDocumentIndexed(KnowledgeDocument document) {
        document.setStatus(KnowledgeDocument.Status.INDEXED);
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
                entity.setErrorMessage("未能从文件中提取到正文内容");
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
        entity.setSourceUrl(entryPath);
        entity.setFileName(fileName);
        entity.setTitle(entryPath);
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
                entity.setErrorMessage("未能从项目文件中提取到正文内容");
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

        KnowledgeIndexTask autoTask = new KnowledgeIndexTask();
        autoTask.setKnowledgeBase(knowledgeBase);
        autoTask.setDocument(saved);
        autoTask.setTaskType(KnowledgeIndexTask.TaskType.REINDEX);
        autoTask.setStatus(KnowledgeIndexTask.Status.PENDING);
        autoTask.setRetryCount(0);
        autoTask.setCreatedAt(Instant.now());
        KnowledgeIndexTask task = executeTask(autoTask);

        if (task.getStatus() == KnowledgeIndexTask.Status.FAILED) {
            KnowledgeDocument failed = knowledgeDocumentRepository.findById(saved.getId()).orElse(saved);
            failed.setStatus(KnowledgeDocument.Status.FAILED);
            failed.setErrorMessage(trimError(task.getErrorMessage()));
            failed.setUpdatedAt(Instant.now());
            knowledgeDocumentRepository.save(failed);
        }

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
            throw new RuntimeException("文档没有可下载的原始文件");
        }
        Path baseDir = ensureStorageDirectory();
        Path path = baseDir.resolve(storagePath).normalize();
        if (!path.startsWith(baseDir)) {
            throw new RuntimeException("非法文件路径");
        }
        if (!Files.exists(path)) {
            throw new RuntimeException("原始文件不存在");
        }
        return path;
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
        return "未命名文档";
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

    private String buildChunkMetadata(KnowledgeDocument document, String text, int index) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("documentId", document.getId());
        metadata.put("title", document.getTitle());
        metadata.put("fileName", document.getFileName());
        metadata.put("sourceType", document.getSourceType() == null ? null : document.getSourceType().name());
        metadata.put("sourceUrl", document.getSourceUrl());
        metadata.put("chunkIndex", index);
        metadata.put("tokenCount", estimateTokens(text));
        return toJson(metadata);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return "{}";
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
            throw new RuntimeException("计算内容哈希失败", ex);
        }
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
            return "未知错误";
        }
        String trimmed = error.replaceAll("[\r\n]+", " ").trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }

    private int estimateTokens(String content) {
        if (!StringUtils.hasText(content)) {
            return 0;
        }
        return Math.max(1, content.length() / 4);
    }

    private List<String> splitIntoChunks(String content, KnowledgeBase.ChunkStrategy strategy) {
        String normalized = normalizeContent(content);
        if (!StringUtils.hasText(normalized)) {
            return List.of();
        }

        KnowledgeBase.ChunkStrategy actualStrategy = strategy == null ? KnowledgeBase.ChunkStrategy.PARAGRAPH : strategy;
        return switch (actualStrategy) {
            case FIXED -> splitFixed(normalized);
            case MARKDOWN -> splitMarkdown(normalized);
            case CUSTOM, PARAGRAPH -> splitByBlocks(normalized);
        };
    }

    private List<String> splitFixed(String content) {
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < content.length()) {
            int end = Math.min(content.length(), start + FIXED_CHUNK_SIZE);
            String chunk = content.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                result.add(chunk);
            }
            if (end >= content.length()) {
                break;
            }
            start = Math.max(end - FIXED_CHUNK_OVERLAP, start + 1);
        }
        return result;
    }

    private List<String> splitMarkdown(String content) {
        String normalized = content.replaceAll("(?m)(^#{1,6}\\s+)", "\n\n$1");
        return splitByBlocks(normalized);
    }

    private List<String> splitByBlocks(String content) {
        List<String> blocks = Arrays.stream(content.split("\n\n+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        return mergeBlocks(blocks);
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
                result.addAll(splitFixed(block));
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

    private KnowledgeDocument loadDocument(Long knowledgeBaseId, Long documentId) {
        KnowledgeDocument document = knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new RuntimeException("知识文档不存在"));
        if (!document.getKnowledgeBase().getId().equals(knowledgeBaseId)) {
            throw new RuntimeException("文档不属于当前知识库");
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
            throw new IllegalArgumentException("知识库请求不能为空");
        }
        if (request.getOwnerId() == null) {
            throw new IllegalArgumentException("ownerId不能为空");
        }
        if (!StringUtils.hasText(request.getName())) {
            throw new IllegalArgumentException("知识库名称不能为空");
        }
    }

    private void validateDocumentRequest(KnowledgeDocumentCreateRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("文档请求不能为空");
        }
        if (!StringUtils.hasText(request.getTitle())
                && !StringUtils.hasText(request.getFileName())
                && !StringUtils.hasText(request.getContentText())) {
            throw new IllegalArgumentException("title、fileName 和 contentText 不能同时为空");
        }
        if (!StringUtils.hasText(request.getContentText())) {
            throw new IllegalArgumentException("contentText不能为空");
        }
    }

    private KnowledgeIndexTask.TaskType resolveTaskType(KnowledgeIndexTaskCreateRequest request) {
        return request == null || request.getTaskType() == null ? KnowledgeIndexTask.TaskType.REINDEX : request.getTaskType();
    }

    private void refreshKnowledgeBaseStats(Long knowledgeBaseId, KnowledgeBase.Status status) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
        List<KnowledgeDocument> docs = knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBaseId);

        int docCount = docs.size();
        int chunkCount = 0;
        Instant lastIndexedAt = null;
        for (KnowledgeDocument doc : docs) {
            List<KnowledgeChunk> chunks = knowledgeChunkRepository.findByDocument_IdOrderByChunkIndexAsc(doc.getId());
            chunkCount += chunks.size();
            if (doc.getIndexedAt() != null && (lastIndexedAt == null || doc.getIndexedAt().isAfter(lastIndexedAt))) {
                lastIndexedAt = doc.getIndexedAt();
            }
        }

        knowledgeBase.setDocCount(docCount);
        knowledgeBase.setChunkCount(chunkCount);
        knowledgeBase.setLastIndexedAt(lastIndexedAt);
        if (status != null) {
            knowledgeBase.setStatus(status);
        } else if (docCount == 0) {
            knowledgeBase.setStatus(KnowledgeBase.Status.DRAFT);
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
                    throw new RuntimeException("ZIP 内单个文件过大: " + targetPath.getFileName());
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
}