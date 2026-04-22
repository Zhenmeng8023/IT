package com.alikeyou.itmoduleai.dto.front.request;

import com.alikeyou.itmoduleai.dto.request.KnowledgeDocumentCreateRequest;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FrontKnowledgeDocumentCreateRequest {

    private KnowledgeDocument.SourceType sourceType;
    private Long sourceRefId;
    private String sourceUrl;
    private String title;
    private String fileName;
    private String mimeType;
    private String contentText;
    private String contentHash;
    private String language;
    private Boolean autoIndex;

    public KnowledgeDocumentCreateRequest toServiceRequest(Long uploadedBy) {
        KnowledgeDocumentCreateRequest target = new KnowledgeDocumentCreateRequest();
        target.setSourceType(sourceType);
        target.setSourceRefId(sourceRefId);
        target.setSourceUrl(sourceUrl);
        target.setTitle(title);
        target.setFileName(fileName);
        target.setMimeType(mimeType);
        target.setContentText(contentText);
        target.setContentHash(contentHash);
        target.setLanguage(language);
        target.setUploadedBy(uploadedBy);
        target.setAutoIndex(autoIndex);
        return target;
    }
}
