package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeSearchDebugRequest {
    private String query;
    private Integer topK;
}
