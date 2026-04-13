package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CodeAnalysisPlan {

    private final AiAnalysisMode mode;
    private final boolean strictGrounding;
    private final int topK;
    private final int graphDepth;
    private final String entryFile;
    private final String symbolHint;
    private final String normalizedQuestion;
    private final List<String> tokens;
    private final List<String> symbolTerms;
    private final List<String> keywordTerms;
    private final List<PlanPhase> phases;

    public boolean isCodeLogic() {
        return mode == AiAnalysisMode.CODE_LOGIC;
    }

    public boolean isCodeLocate() {
        return mode == AiAnalysisMode.CODE_LOCATE;
    }

    public boolean isDocQa() {
        return mode == AiAnalysisMode.DOC_QA;
    }

    public record PlanPhase(
            AiRetrievalLog.StageCode stageCode,
            String phase,
            int order,
            String description
    ) {}
}
