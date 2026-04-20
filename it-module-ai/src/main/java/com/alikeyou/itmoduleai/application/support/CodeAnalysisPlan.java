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
    private final RetrievalPolicy retrievalPolicy;

    public boolean isCodeLogic() {
        return mode == AiAnalysisMode.CODE_LOGIC;
    }

    public boolean isCodeLocate() {
        return mode == AiAnalysisMode.CODE_LOCATE;
    }

    public boolean isDocQa() {
        return mode == AiAnalysisMode.DOC_QA;
    }

    public int recallLimit(int maxCandidates) {
        int top = Math.max(0, topK);
        RetrievalPolicy policy = RetrievalPolicy.safe(retrievalPolicy);
        int desired = Math.max(top, Math.max(policy.minRecallCandidates(), top * policy.recallMultiplier()));
        return Math.min(Math.max(0, maxCandidates), desired);
    }

    public int rerankPoolLimit(int maxCandidates) {
        int top = Math.max(0, topK);
        RetrievalPolicy policy = RetrievalPolicy.safe(retrievalPolicy);
        int desired = Math.max(top, top * policy.rerankPoolMultiplier());
        return Math.min(Math.max(0, maxCandidates), desired);
    }

    public record PlanPhase(
            AiRetrievalLog.StageCode stageCode,
            String phase,
            int order,
            String description
    ) {}

    public record RetrievalPolicy(
            String profile,
            double vectorWeight,
            double keywordWeight,
            double declarationBoost,
            double symbolBoost,
            double graphWeight,
            double adjacentBoost,
            double contentBoost,
            double pathOnlyPenalty,
            double lowContentPenalty,
            double fallbackPenalty,
            double minRerankScore,
            int minStrictEvidence,
            int recallMultiplier,
            int rerankPoolMultiplier,
            int minRecallCandidates,
            int maxPerDocument,
            boolean fallbackEnabled,
            boolean allowPathOnly
    ) {
        static RetrievalPolicy safe(RetrievalPolicy policy) {
            return policy == null ? defaultPolicy() : policy;
        }

        static RetrievalPolicy defaultPolicy() {
            return new RetrievalPolicy(
                    "doc_qa",
                    0.62D,
                    0.22D,
                    0D,
                    1D,
                    0.20D,
                    2D,
                    10D,
                    -32D,
                    -14D,
                    -18D,
                    3D,
                    1,
                    36,
                    6,
                    50,
                    2,
                    true,
                    false
            );
        }
    }
}
