package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface KnowledgeImportTaskService {

    KnowledgeImportTask createZipImportTask(Long knowledgeBaseId, MultipartFile file, KnowledgeDocumentCreateRequest request);

    KnowledgeImportTask getTask(Long taskId);

    List<KnowledgeImportTask> listByKnowledgeBase(Long knowledgeBaseId);

    KnowledgeImportTask cancelTask(Long taskId);
}
