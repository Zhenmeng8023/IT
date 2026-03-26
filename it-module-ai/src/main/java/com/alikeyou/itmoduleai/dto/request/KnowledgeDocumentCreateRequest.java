package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeDocumentCreateRequest {

    private KnowledgeDocument.SourceType sourceType;
    private Long sourceRefId;
    private String title;
    private String contentText;
    private String contentHash;
}
