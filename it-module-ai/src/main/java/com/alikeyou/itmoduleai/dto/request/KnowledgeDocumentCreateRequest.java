package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDocumentCreateRequest {

    private KnowledgeDocument.SourceType sourceType;
    private Long sourceRefId;
    private String sourceUrl;
    private String title;
    private String fileName;
    private String mimeType;
    private String contentText;
    private String contentHash;
    private String language;
    private Long uploadedBy;
    private Boolean autoIndex;
}
