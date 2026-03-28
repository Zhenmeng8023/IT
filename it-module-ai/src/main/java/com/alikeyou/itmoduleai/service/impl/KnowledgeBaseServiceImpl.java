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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
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
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
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

        KnowledgeBase entity = new KnowledgeBase();
        entity.setScopeType(request.getScopeType() == null ? KnowledgeBase.ScopeType.PERSONAL : request.getScopeType());
        entity.setProjectId(request.getProjectId());
        entity.setOwnerId(request.getOwnerId());
        entity.setName(request.getName().trim());
        entity.setDescription(trimToNull(request.getDescription()));
        entity.setSourceType(request.getSourceType() == null ? KnowledgeBase.SourceType.MANUAL : request.getSourceType());
        entity.setEmbeddingProvider(trimToNull(request.getEmbeddingProvider()));
        entity.setEmbeddingModel(trimToNull(request.getEmbeddingModel()));
        entity.setChunkStrategy(request.getChunkStrategy() == null ? KnowledgeBase.ChunkStrategy.PARAGRAPH : request.getChunkStrategy());
        entity.setDefaultTopK(normalizeTopK(request.getDefaultTopK()));
        entity.setVisibility(request.getVisibility() == null ? KnowledgeBase.Visibility.PRIVATE : request.getVisibility());
        entity.setStatus(KnowledgeBase.Status.DRAFT);
        entity.setDocCount(0);
        entity.setChunkCount(0);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

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

        if (request.getScopeType() != null) {
            entity.setScopeType(request.getScopeType());
        }
        if (request.getProjectId() != null) {
            entity.setProjectId(request.getProjectId());
        }
        if (request.getOwnerId() != null) {
            entity.setOwnerId(request.getOwnerId());
        }
        if (StringUtils.hasText(request.getName())) {
            entity.setName(request.getName().trim());
        }
        if (request.getDescription() != null) {
            entity.setDescription(trimToNull(request.getDescription()));
        }
        if (request.getSourceType() != null) {
            entity.setSourceType(request.getSourceType());
        }
        if (request.getEmbeddingProvider() != null) {
            entity.setEmbeddingProvider(trimToNull(request.getEmbeddingProvider()));
        }
        if (request.getEmbeddingModel() != null) {
            entity.setEmbeddingModel(trimToNull(request.getEmbeddingModel()));
        }
        if (request.getChunkStrategy() != null) {
            entity.setChunkStrategy(request.getChunkStrategy());
        }
        if (request.getDefaultTopK() != null) {
            entity.setDefaultTopK(normalizeTopK(request.getDefaultTopK()));
        }
        if (request.getVisibility() != null) {
            entity.setVisibility(request.getVisibility());
        }
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
        KnowledgeDocument entity = new KnowledgeDocument();
        entity.setKnowledgeBase(knowledgeBase);
        entity.setSourceType(request.getSourceType() == null ? KnowledgeDocument.SourceType.MANUAL : request.getSourceType());
        entity.setSourceRefId(request.getSourceRefId());
        entity.setSourceUrl(trimToNull(request.getSourceUrl()));
        entity.setTitle(resolveDocumentTitle(request.getTitle(), null, request.getContentText()));
        entity.setFileName(trimToNull(request.getFileName()));
        entity.setMimeType(trimToNull(request.getMimeType()));
        entity.setStoragePath(null);
        entity.setContentText(normalizeContent(request.getContentText()));
        entity.setContentHash(StringUtils.hasText(request.getContentHash()) ? request.getContentHash().trim() : sha256(entity.getContentText()));
        entity.setLanguage(trimToNull(request.getLanguage()));
        entity.setErrorMessage(null);
        entity.setUploadedBy(request.getUploadedBy());
        entity.setStatus(KnowledgeDocument.Status.UPLOADED);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());

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
                Path path = resolveStoredPath(document.getStoragePath());
                if (Files.exists(path)) {
                    String fileName = StringUtils.hasText(document.getFileName()) ? document.getFileName() : path.getFileName().toString();
                    String contentType = resolveMimeType(path, fileName, document.getMimeType());
                    return new KnowledgeDocumentBinary(fileName, contentType, Files.readAllBytes(path));
                }
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
                KnowledgeDocumentBinary binary = downloadDocument(document.getId());
                String entryName = uniqueEntryName(binary.getFileName(), usedNames);
                zos.putNextEntry(new ZipEntry(entryName));
                zos.write(binary.getContent());
                zos.closeEntry();
            }
            zos.finish();
            String zipName = safeFileComponent(knowledgeBase.getName()) + "-documents.zip";
            return new KnowledgeDocumentBinary(zipName, "application/zip", bos.toByteArray());
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
            savedTask.setFinishedAt(Instant.now());
            knowledgeIndexTaskRepository.save(savedTask);
        } catch (Exception ex) {
            savedTask.setStatus(KnowledgeIndexTask.Status.FAILED);
            savedTask.setErrorMessage(trimError(ex.getMessage()));
            savedTask.setFinishedAt(Instant.now());
            knowledgeIndexTaskRepository.save(savedTask);

            if (savedTask.getDocument() != null) {
                KnowledgeDocument document = savedTask.getDocument();
                document.setStatus(KnowledgeDocument.Status.FAILED);
                document.setErrorMessage(trimError(savedTask.getErrorMessage()));
                document.setUpdatedAt(Instant.now());
                knowledgeDocumentRepository.save(document);
            }

            KnowledgeBase knowledgeBase = savedTask.getKnowledgeBase();
            knowledgeBase.setStatus(KnowledgeBase.Status.FAILED);
            knowledgeBase.setUpdatedAt(Instant.now());
            knowledgeBaseRepository.save(knowledgeBase);
        }

        return savedTask;
    }

    private void runTask(KnowledgeIndexTask task) {
        KnowledgeBase knowledgeBase = task.getKnowledgeBase();
        if (knowledgeBase == null || knowledgeBase.getId() == null) {
            throw new IllegalArgumentException("知识库不能为空");
        }

        if (task.getDocument() != null) {
            runTaskForDocument(knowledgeBase, task.getDocument(), task.getTaskType());
        } else {
            List<KnowledgeDocument> documents = knowledgeDocumentRepository.findByKnowledgeBase_IdOrderByIdAsc(knowledgeBase.getId());
            for (KnowledgeDocument document : documents) {
                runTaskForDocument(knowledgeBase, document, task.getTaskType());
            }
        }

        Instant indexedAt = task.getTaskType() == KnowledgeIndexTask.TaskType.PARSE ? null : Instant.now();
        refreshKnowledgeBaseStats(knowledgeBase.getId(), indexedAt);
    }

    private void runTaskForDocument(KnowledgeBase knowledgeBase,
                                    KnowledgeDocument document,
                                    KnowledgeIndexTask.TaskType taskType) {
        KnowledgeDocument managedDocument = loadDocument(knowledgeBase.getId(), document.getId());

        switch (taskType) {
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

            entity.setTitle(resolveUploadedTitle(request == null ? null : request.getTitle(), originalFileName, normalizedText, singleFile));
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
        Path path = Paths.get(storagePath);
        if (path.isAbsolute()) {
            return path.normalize();
        }
        return ensureStorageDirectory().resolve(storagePath).normalize();
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
            String text = new String(bytes, StandardCharsets.UTF_8);
            return stripUtf8Bom(text);
        }
        throw new IllegalArgumentException("暂不支持解析该文件类型: " + ext);
    }

    private boolean isSupportedTextExtension(String ext) {
        return Set.of(
                "txt", "md", "markdown", "json", "csv", "js", "ts", "java", "xml", "html", "htm",
                "css", "vue", "sql", "yml", "yaml", "properties", "log", "text"
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
        return fileName.substring(fileName.lastIndexOf('.') + 1).trim().toLowerCase();
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
        String guessed = URLConnection.guessContentTypeFromName(fileName);
        if (StringUtils.hasText(guessed)) {
            return guessed;
        }
        return StringUtils.hasText(fallback) ? fallback : "application/octet-stream";
    }

    private String resolveUploadedTitle(String requestTitle, String originalFileName, String contentText, boolean singleFile) {
        if (singleFile && StringUtils.hasText(requestTitle)) {
            return requestTitle.trim();
        }
        return resolveDocumentTitle(null, originalFileName, contentText);
    }

    private String ensureTextFileName(String baseName) {
        String safe = safeFileComponent(baseName);
        if (!safe.endsWith(".txt")) {
            safe = safe + ".txt";
        }
        return safe;
    }

    private String safeFileComponent(String name) {
        String candidate = sanitizeFileName(name);
        return candidate.isBlank() ? "knowledge-document" : candidate;
    }

    private String uniqueEntryName(String name, Set<String> usedNames) {
        String safeName = safeFileComponent(name);
        if (!usedNames.contains(safeName)) {
            usedNames.add(safeName);
            return safeName;
        }
        String ext = "";
        String base = safeName;
        int dotIndex = safeName.lastIndexOf('.');
        if (dotIndex > 0) {
            base = safeName.substring(0, dotIndex);
            ext = safeName.substring(dotIndex);
        }
        int seq = 2;
        while (true) {
            String candidate = base + "-" + seq + ext;
            if (!usedNames.contains(candidate)) {
                usedNames.add(candidate);
                return candidate;
            }
            seq++;
        }
    }

    private void refreshKnowledgeBaseStats(Long knowledgeBaseId, Instant indexedAt) {
        KnowledgeBase knowledgeBase = getById(knowledgeBaseId);
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

        KnowledgeBaseMember owner = new KnowledgeBaseMember();
        owner.setKnowledgeBase(knowledgeBase);
        owner.setUserId(knowledgeBase.getOwnerId());
        owner.setRoleCode(KnowledgeBaseMember.RoleCode.OWNER);
        owner.setGrantedBy(knowledgeBase.getOwnerId());
        owner.setCreatedAt(Instant.now());
        knowledgeBaseMemberRepository.save(owner);
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
        if (request == null || request.getTaskType() == null) {
            return KnowledgeIndexTask.TaskType.REINDEX;
        }
        return request.getTaskType();
    }

    private int normalizeTopK(Integer topK) {
        if (topK == null) {
            return DEFAULT_TOP_K;
        }
        return Math.max(1, Math.min(topK, 20));
    }

    private String resolveDocumentTitle(String title, String fileName, String contentText) {
        if (StringUtils.hasText(title)) {
            return title.trim();
        }
        if (StringUtils.hasText(fileName)) {
            String sanitized = sanitizeFileName(fileName);
            int dotIndex = sanitized.lastIndexOf('.');
            return dotIndex > 0 ? sanitized.substring(0, dotIndex) : sanitized;
        }
        String normalized = normalizeContent(contentText);
        if (!StringUtils.hasText(normalized)) {
            return "未命名文档";
        }
        return normalized.length() <= 30 ? normalized : normalized.substring(0, 30);
    }

    private String normalizeContent(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n');
        List<String> lines = normalized.lines()
                .map(String::stripTrailing)
                .toList();
        return String.join("\n", lines).trim();
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
        String normalized = content.replaceAll("(?m)(^#{1,6}\\s+)", "\n\n$1");
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

    private String buildChunkMetadata(KnowledgeBase knowledgeBase,
                                      KnowledgeDocument document,
                                      Integer chunkIndex,
                                      String content) {
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
        try {
            return objectMapper.writeValueAsString(metadata);
        } catch (JsonProcessingException e) {
            return "{\"documentId\":" + document.getId() + ",\"chunkIndex\":" + chunkIndex + "}";
        }
    }

    private String sha256(String text) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                String hex = Integer.toHexString(b & 0xff);
                if (hex.length() == 1) {
                    sb.append('0');
                }
                sb.append(hex);
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256不可用");
        }
    }
}