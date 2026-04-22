package com.alikeyou.itmoduleai.dto.front.response;

import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class FrontKnowledgeImportTaskResponse {

    private Long id;
    private Long knowledgeBaseId;
    private String zipName;
    private KnowledgeImportTask.Status status;
    private KnowledgeImportTask.Stage currentStage;
    private Integer totalFiles;
    private Integer scannedFiles;
    private Integer importedFiles;
    private Integer skippedFiles;
    private Integer failedFiles;
    private Integer progressPercent;
    private String currentFile;
    private String errorMessage;
    private Boolean cancelRequested;
    private Long createdBy;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant finishedAt;

    public static FrontKnowledgeImportTaskResponse from(KnowledgeImportTask entity) {
        if (entity == null) {
            return null;
        }
        return FrontKnowledgeImportTaskResponse.builder()
                .id(entity.getId())
                .knowledgeBaseId(entity.getKnowledgeBaseId())
                .zipName(entity.getZipName())
                .status(entity.getStatus())
                .currentStage(entity.getCurrentStage())
                .totalFiles(entity.getTotalFiles())
                .scannedFiles(entity.getScannedFiles())
                .importedFiles(entity.getImportedFiles())
                .skippedFiles(entity.getSkippedFiles())
                .failedFiles(entity.getFailedFiles())
                .progressPercent(entity.getProgressPercent())
                .currentFile(entity.getCurrentFile())
                .errorMessage(entity.getErrorMessage())
                .cancelRequested(entity.getCancelRequested())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .finishedAt(entity.getFinishedAt())
                .build();
    }
}
