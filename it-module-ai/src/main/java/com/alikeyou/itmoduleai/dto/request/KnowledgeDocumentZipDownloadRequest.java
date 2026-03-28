package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class KnowledgeDocumentZipDownloadRequest {

    private List<Long> documentIds;
}
