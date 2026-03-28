package com.alikeyou.itmoduleai.dto.common;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class KnowledgeDocumentBinary {

    private final String fileName;
    private final String contentType;
    private final byte[] content;
}
