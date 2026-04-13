package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class CodeRetrievalPlanner {

    public CodeAnalysisPlan buildPlan(AiAnalysisMode mode,
                                      boolean strictGrounding,
                                      int topK,
                                      Integer traceDepth,
                                      String entryFile,
                                      String symbolHint,
                                      String normalizedQuestion,
                                      List<String> tokens,
                                      List<String> keywordTerms) {
        AiAnalysisMode resolvedMode = mode == null ? AiAnalysisMode.DOC_QA : mode;
        int graphDepth = resolvedMode == AiAnalysisMode.CODE_LOGIC ? normalizeTraceDepth(traceDepth) : 0;
        List<String> symbolTerms = buildSymbolTerms(symbolHint, entryFile, normalizedQuestion, tokens);
        return CodeAnalysisPlan.builder()
                .mode(resolvedMode)
                .strictGrounding(strictGrounding)
                .topK(Math.max(0, topK))
                .graphDepth(graphDepth)
                .entryFile(clean(entryFile))
                .symbolHint(clean(symbolHint))
                .normalizedQuestion(normalizedQuestion)
                .tokens(tokens == null ? List.of() : tokens)
                .symbolTerms(symbolTerms)
                .keywordTerms(keywordTerms == null ? List.of() : keywordTerms)
                .phases(phases(resolvedMode, graphDepth))
                .build();
    }

    private List<String> buildSymbolTerms(String symbolHint,
                                          String entryFile,
                                          String normalizedQuestion,
                                          List<String> tokens) {
        Set<String> terms = new LinkedHashSet<>();
        addTerm(terms, symbolHint);
        addLastPathSegment(terms, entryFile);
        if (tokens != null) {
            tokens.stream()
                    .filter(StringUtils::hasText)
                    .filter(token -> token.length() >= 3 || token.codePoints().anyMatch(this::isHan))
                    .sorted(Comparator.comparingInt(String::length).reversed())
                    .forEach(terms::add);
        }
        if (StringUtils.hasText(normalizedQuestion) && normalizedQuestion.length() <= 80) {
            terms.add(normalizedQuestion);
        }
        return terms.stream().limit(5).toList();
    }

    private List<CodeAnalysisPlan.PlanPhase> phases(AiAnalysisMode mode, int graphDepth) {
        List<CodeAnalysisPlan.PlanPhase> phases = new ArrayList<>();
        phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.PLAN, "plan", 0, "Classify code retrieval intent and build recall plan."));
        if (mode == AiAnalysisMode.CODE_LOGIC) {
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.DECLARATION_FIRST, "declaration_first", 1, "Recall symbol declarations before generic file or keyword matches."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.GRAPH_EXPAND, "graph_expand", 2, "Expand resolved references around declaration hits, depth=" + graphDepth + "."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RECALL, "adjacent_chunk_expand", 3, "Expand neighboring chunks for local implementation context."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RERANK, "rerank", 4, "Rerank by declaration, graph, symbol, keyword, and vector evidence."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.GROUND, "grounding_gate", 5, "Reject strict answers when grounding evidence is insufficient."));
            return phases;
        }
        if (mode == AiAnalysisMode.CODE_LOCATE) {
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.DECLARATION_FIRST, "declaration_first", 1, "Prefer symbol declarations for code location questions."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RECALL, "keyword_path_recall", 2, "Use keyword and path recall as secondary evidence."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RERANK, "rerank", 3, "Rerank declaration and path candidates."));
            phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.GROUND, "grounding_gate", 4, "Apply strict grounding rules when enabled."));
            return phases;
        }
        phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RECALL, "hybrid_recall", 1, "Use vector and keyword retrieval for document QA."));
        phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.RERANK, "rerank", 2, "Rerank and diversify document QA evidence."));
        phases.add(new CodeAnalysisPlan.PlanPhase(AiRetrievalLog.StageCode.GROUND, "grounding_gate", 3, "Apply strict grounding rules when enabled."));
        return phases;
    }

    private int normalizeTraceDepth(Integer traceDepth) {
        if (traceDepth == null) {
            return 1;
        }
        return Math.max(0, Math.min(traceDepth, 3));
    }

    private void addTerm(Set<String> terms, String value) {
        String text = clean(value);
        if (!StringUtils.hasText(text)) {
            return;
        }
        terms.add(text);
        int dot = Math.max(text.lastIndexOf('.'), text.lastIndexOf('#'));
        if (dot >= 0 && dot + 1 < text.length()) {
            terms.add(text.substring(dot + 1));
        }
    }

    private void addLastPathSegment(Set<String> terms, String value) {
        String text = clean(value);
        if (!StringUtils.hasText(text)) {
            return;
        }
        terms.add(text);
        String normalized = text.replace('\\', '/');
        int slash = normalized.lastIndexOf('/');
        String fileName = slash >= 0 ? normalized.substring(slash + 1) : normalized;
        if (StringUtils.hasText(fileName)) {
            terms.add(fileName);
            int dot = fileName.indexOf('.');
            if (dot > 0) {
                terms.add(fileName.substring(0, dot));
            }
        }
    }

    private boolean isHan(int codePoint) {
        return Character.UnicodeScript.of(codePoint) == Character.UnicodeScript.HAN;
    }

    private String clean(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
