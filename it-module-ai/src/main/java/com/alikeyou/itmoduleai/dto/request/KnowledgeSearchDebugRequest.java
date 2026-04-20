package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeSearchDebugRequest {
    private String query;
    private Integer topK;
    private AiAnalysisMode mode;
    private Boolean strictGrounding;
    private String entryFile;
    private String symbolHint;
    private Integer traceDepth;
    private String actionCode;
}
