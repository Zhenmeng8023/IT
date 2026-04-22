package com.alikeyou.itmoduleai.dto.front.response;

import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FrontKnowledgeDocumentResponse {

    private Long id;
    private Long knowledgeBaseId;
    private KnowledgeDocument.SourceType sourceType;
    private Long sourceRefId;
    private String sourceUrl;
    private Long projectId;
    private Long repositoryId;
    private Long branchId;
    private Long commitId;
    private Long projectFileId;
    private Long projectFileVersionId;
    private KnowledgeDocument.DocKind docKind;
    private String filePath;
    private String parserName;
    private String parserVersion;
    private KnowledgeDocument.ParseStatus parseStatus;
    private KnowledgeDocument.SymbolIndexStatus symbolIndexStatus;
    private Integer symbolCount;
    private Integer referenceCount;
    private String metadataJson;
    private Instant sourceUpdatedAt;
    private String title;
    private String fileName;
    private String archiveName;
    private String archiveEntryPath;
    private String importBatchId;
    private String mimeType;
    private String language;
    private KnowledgeDocument.Status status;
    private String errorMessage;
    private Long uploadedBy;
    private Instant indexedAt;
    private Instant createdAt;
    private Instant updatedAt;

    public static FrontKnowledgeDocumentResponse from(KnowledgeDocument entity) {
        if (entity == null) {
            return null;
        }
        return FrontKnowledgeDocumentResponse.builder()
                .id(entity.getId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .sourceType(entity.getSourceType())
                .sourceRefId(entity.getSourceRefId())
                .sourceUrl(entity.getSourceUrl())
                .projectId(entity.getProjectId())
                .repositoryId(entity.getRepositoryId())
                .branchId(entity.getBranchId())
                .commitId(entity.getCommitId())
                .projectFileId(entity.getProjectFileId())
                .projectFileVersionId(entity.getProjectFileVersionId())
                .docKind(entity.getDocKind())
                .filePath(entity.getFilePath())
                .parserName(entity.getParserName())
                .parserVersion(entity.getParserVersion())
                .parseStatus(entity.getParseStatus())
                .symbolIndexStatus(entity.getSymbolIndexStatus())
                .symbolCount(entity.getSymbolCount())
                .referenceCount(entity.getReferenceCount())
                .metadataJson(entity.getMetadataJson())
                .sourceUpdatedAt(entity.getSourceUpdatedAt())
                .title(entity.getTitle())
                .fileName(entity.getFileName())
                .archiveName(entity.getArchiveName())
                .archiveEntryPath(entity.getArchiveEntryPath())
                .importBatchId(entity.getImportBatchId())
                .mimeType(entity.getMimeType())
                .language(entity.getLanguage())
                .status(entity.getStatus())
                .errorMessage(entity.getErrorMessage())
                .uploadedBy(entity.getUploadedBy())
                .indexedAt(entity.getIndexedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
