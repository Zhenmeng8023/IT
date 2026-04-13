package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import lombok.Getter;

import java.util.List;

@Getter
public class CodeEvidencePack {

    private final CodeAnalysisPlan plan;
    private final List<KnowledgeRetrievalHit> hits;
    private final GroundingStatus groundingStatus;
    private final boolean refused;
    private final String refusalReason;
    private final String refusalMessage;

    private CodeEvidencePack(CodeAnalysisPlan plan,
                             List<KnowledgeRetrievalHit> hits,
                             GroundingStatus groundingStatus,
                             boolean refused,
                             String refusalReason) {
        this.plan = plan;
        this.hits = hits == null ? List.of() : hits;
        this.groundingStatus = groundingStatus == null ? GroundingStatus.NOT_CHECKED : groundingStatus;
        this.refused = refused;
        this.refusalReason = refusalReason;
        this.refusalMessage = refused
                ? "当前检索证据不足，strictGrounding=true 时不能基于不完整证据回答。" +
                (refusalReason == null ? "" : " 原因：" + refusalReason)
                : null;
    }

    public static CodeEvidencePack pass(CodeAnalysisPlan plan, List<KnowledgeRetrievalHit> hits, GroundingStatus status) {
        return new CodeEvidencePack(plan, hits, status, false, null);
    }

    public static CodeEvidencePack refuse(CodeAnalysisPlan plan, List<KnowledgeRetrievalHit> hits, String reason) {
        return new CodeEvidencePack(plan, hits, GroundingStatus.STRICT_FAIL, true, reason);
    }
}
