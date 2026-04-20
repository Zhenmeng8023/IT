package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.entity.AiCodeSymbol;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class CodeReranker {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final ObjectMapper objectMapper;

    public List<KnowledgeRetrievalHit> rerank(CodeAnalysisPlan plan, List<KnowledgeRetrievalHit> hits, int limit) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }
        List<KnowledgeRetrievalHit> ranked = new ArrayList<>();
        for (KnowledgeRetrievalHit hit : hits) {
            if (hit == null) {
                continue;
            }
            applyScore(plan, hit);
            CodeAnalysisPlan.RetrievalPolicy policy = CodeAnalysisPlan.RetrievalPolicy.safe(plan == null ? null : plan.getRetrievalPolicy());
            boolean pathOnly = isPathOnly(plan, hit);
            boolean include = positive(hit.getRerankScore())
                    && (policy.allowPathOnly() || !pathOnly || declarationPriority(hit) || symbolMatches(plan, hit));
            if (include || (plan != null && plan.isStrictGrounding() && pathOnly)) {
                ranked.add(hit);
            }
        }
        ranked.sort(Comparator.comparing(KnowledgeRetrievalHit::getRerankScore, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(hit -> declarationPriority(hit) ? 0 : 1)
                .thenComparing(KnowledgeRetrievalHit::getDocumentId, Comparator.nullsLast(Long::compareTo))
                .thenComparing(KnowledgeRetrievalHit::getChunkIndex, Comparator.nullsLast(Integer::compareTo)));
        List<KnowledgeRetrievalHit> result = new ArrayList<>();
        int max = Math.max(0, limit);
        int rank = 1;
        for (KnowledgeRetrievalHit hit : ranked) {
            if (result.size() >= max) {
                break;
            }
            hit.setRankNo(rank++);
            result.add(hit);
        }
        return result;
    }

    private void applyScore(CodeAnalysisPlan plan, KnowledgeRetrievalHit hit) {
        CodeAnalysisPlan.RetrievalPolicy policy = CodeAnalysisPlan.RetrievalPolicy.safe(plan == null ? null : plan.getRetrievalPolicy());
        BigDecimal keyword = value(hit.getKeywordScore());
        BigDecimal vector = value(hit.getVectorScore());
        BigDecimal graph = value(hit.getGraphScore());
        boolean declaration = declarationPriority(hit);
        boolean symbolMatch = symbolMatches(plan, hit);
        boolean contentMatch = contentMatches(plan, hit);
        boolean pathOnly = isPathOnly(plan, hit);
        boolean lowContent = isLowContent(hit) && !declaration && !symbolMatch && !contentMatch;
        boolean fallback = Objects.equals(hit.getPhase(), "fallback_recall");

        double keywordForRanking = keyword.doubleValue();
        if (pathOnly) {
            keywordForRanking = Math.min(keywordForRanking, 2D);
        } else if (plan != null && plan.isDocQa() && !contentMatch) {
            keywordForRanking = Math.min(keywordForRanking, 4D);
        }

        double vectorContribution = vector.doubleValue() * policy.vectorWeight();
        double keywordContribution = keywordForRanking * policy.keywordWeight();
        double graphContribution = graph.doubleValue() * policy.graphWeight();
        double declarationBoost = declaration ? policy.declarationBoost() : 0D;
        if (plan != null && plan.isDocQa() && declaration && !symbolMatch && !contentMatch) {
            declarationBoost = 0D;
        }
        double symbolBoost = symbolMatch ? policy.symbolBoost() : 0D;
        double contentBoost = contentMatch ? policy.contentBoost() : 0D;
        double adjacentBoost = Objects.equals(hit.getPhase(), "adjacent_chunk_expand") ? policy.adjacentBoost() : 0D;
        double pathOnlyPenalty = pathOnly ? policy.pathOnlyPenalty() : 0D;
        double lowContentPenalty = lowContent ? policy.lowContentPenalty() : 0D;
        double fallbackPenalty = fallback ? policy.fallbackPenalty() : 0D;

        double rerank = vectorContribution + keywordContribution + graphContribution + declarationBoost
                + symbolBoost + contentBoost + adjacentBoost + pathOnlyPenalty + lowContentPenalty + fallbackPenalty;
        if (rerank < policy.minRerankScore() && !(plan != null && plan.isStrictGrounding() && pathOnly)) {
            rerank = 0D;
        }
        BigDecimal rerankScore = BigDecimal.valueOf(Math.max(0D, rerank)).setScale(6, RoundingMode.HALF_UP);
        hit.setRerankScore(rerankScore);
        hit.setScore(rerankScore);
        hit.setScoreDetailJson(toJson(scoreDetail(plan, policy, hit, keyword, keywordForRanking, vector, graph,
                vectorContribution, keywordContribution, graphContribution, declarationBoost, symbolBoost,
                contentBoost, adjacentBoost, pathOnlyPenalty, lowContentPenalty, fallbackPenalty, rerankScore,
                declaration, symbolMatch, contentMatch, pathOnly, lowContent, fallback, finalReasons(
                        keywordForRanking, vector, graph, declaration, symbolMatch, contentMatch, pathOnly, lowContent, fallback, hit))));
    }

    private Map<String, Object> scoreDetail(CodeAnalysisPlan plan,
                                            CodeAnalysisPlan.RetrievalPolicy policy,
                                            KnowledgeRetrievalHit hit,
                                            BigDecimal keyword,
                                            double keywordForRanking,
                                            BigDecimal vector,
                                            BigDecimal graph,
                                            double vectorContribution,
                                            double keywordContribution,
                                            double graphContribution,
                                            double declarationBoost,
                                            double symbolBoost,
                                            double contentBoost,
                                            double adjacentBoost,
                                            double pathOnlyPenalty,
                                            double lowContentPenalty,
                                            double fallbackPenalty,
                                            BigDecimal rerankScore,
                                            boolean declaration,
                                            boolean symbolMatch,
                                            boolean contentMatch,
                                            boolean pathOnly,
                                            boolean lowContent,
                                            boolean fallback,
                                            List<String> finalReasons) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("profile", policy.profile());
        detail.put("mode", plan == null || plan.getMode() == null ? null : plan.getMode().name());
        detail.put("strictGrounding", plan != null && plan.isStrictGrounding());
        detail.put("stageCode", hit.getStageCode() == null ? null : hit.getStageCode().name());
        detail.put("phase", hit.getPhase());
        detail.put("candidateSource", hit.getCandidateSource() == null ? null : hit.getCandidateSource().name());
        detail.put("keyword", keyword);
        detail.put("keywordForRanking", round(keywordForRanking));
        detail.put("vector", vector);
        detail.put("graph", graph);
        detail.put("vectorContribution", round(vectorContribution));
        detail.put("keywordContribution", round(keywordContribution));
        detail.put("graphContribution", round(graphContribution));
        detail.put("declarationBoost", round(declarationBoost));
        detail.put("symbolBoost", round(symbolBoost));
        detail.put("contentBoost", round(contentBoost));
        detail.put("adjacentBoost", round(adjacentBoost));
        detail.put("pathOnlyPenalty", round(pathOnlyPenalty));
        detail.put("lowContentPenalty", round(lowContentPenalty));
        detail.put("fallbackPenalty", round(fallbackPenalty));
        detail.put("declaration", declaration);
        detail.put("symbolMatch", symbolMatch);
        detail.put("contentMatch", contentMatch);
        detail.put("pathOnly", pathOnly);
        detail.put("lowContent", lowContent);
        detail.put("fallback", fallback);
        detail.put("finalReasons", finalReasons);
        detail.put("rerank", rerankScore);
        return detail;
    }

    private List<String> finalReasons(double keywordForRanking,
                                      BigDecimal vector,
                                      BigDecimal graph,
                                      boolean declaration,
                                      boolean symbolMatch,
                                      boolean contentMatch,
                                      boolean pathOnly,
                                      boolean lowContent,
                                      boolean fallback,
                                      KnowledgeRetrievalHit hit) {
        Set<String> reasons = new LinkedHashSet<>();
        if (positive(vector)) reasons.add("semantic_vector_similarity");
        if (keywordForRanking > 0D) reasons.add(contentMatch ? "keyword_content_match" : "keyword_metadata_match");
        if (declaration) reasons.add("declaration_match");
        if (symbolMatch) reasons.add("symbol_match");
        if (positive(graph)) reasons.add("graph_context");
        if (Objects.equals(hit.getPhase(), "adjacent_chunk_expand")) reasons.add("adjacent_context");
        if (fallback) reasons.add("fallback_candidate");
        if (pathOnly) reasons.add("penalized_path_only");
        if (lowContent) reasons.add("penalized_low_content");
        return new ArrayList<>(reasons);
    }

    private boolean declarationPriority(KnowledgeRetrievalHit hit) {
        AiCodeSymbol symbol = hit.getSymbol();
        if (symbol != null && Boolean.TRUE.equals(symbol.getIsDeclaration())) {
            return true;
        }
        return hit.getStageCode() == AiRetrievalLog.StageCode.DECLARATION_FIRST;
    }

    private boolean symbolMatches(CodeAnalysisPlan plan, KnowledgeRetrievalHit hit) {
        if (plan == null || plan.getSymbolTerms() == null || plan.getSymbolTerms().isEmpty()) {
            return false;
        }
        String symbolText = normalize(hit.getSymbolName() + " " + hit.getSectionName());
        AiCodeSymbol symbol = hit.getSymbol();
        if (symbol != null) {
            symbolText = normalize(symbolText + " " + symbol.getQualifiedName() + " " + symbol.getSignature());
        }
        for (String term : plan.getSymbolTerms()) {
            String normalized = normalize(term);
            if (StringUtils.hasText(normalized) && symbolText.contains(normalized)) {
                return true;
            }
        }
        return false;
    }

    private boolean contentMatches(CodeAnalysisPlan plan, KnowledgeRetrievalHit hit) {
        if (plan == null || plan.getTokens() == null || plan.getTokens().isEmpty()) {
            return false;
        }
        String content = normalize(firstText(hit.getSnippet(), hit.getChunkContent()));
        if (!StringUtils.hasText(content)) {
            return false;
        }
        int matches = 0;
        for (String token : plan.getTokens()) {
            if (StringUtils.hasText(token) && content.contains(normalize(token))) {
                matches++;
            }
        }
        return matches >= Math.min(2, plan.getTokens().size());
    }

    public boolean isPathOnly(CodeAnalysisPlan plan, KnowledgeRetrievalHit hit) {
        if (hit == null) {
            return true;
        }
        if (positive(hit.getVectorScore()) || positive(hit.getGraphScore())) {
            return false;
        }
        if (hit.getSymbol() != null || StringUtils.hasText(hit.getSymbolName())) {
            return false;
        }
        String content = normalize(firstText(hit.getSnippet(), hit.getChunkContent()));
        if (plan != null && plan.getTokens() != null) {
            for (String token : plan.getTokens()) {
                String normalized = normalize(token);
                if (StringUtils.hasText(normalized) && content.contains(normalized)) {
                    return false;
                }
            }
        }
        return StringUtils.hasText(firstText(hit.getPath(), hit.getFileName(), hit.getArchiveEntryPath()));
    }

    private boolean isLowContent(KnowledgeRetrievalHit hit) {
        String content = normalize(firstText(hit == null ? null : hit.getSnippet(), hit == null ? null : hit.getChunkContent()));
        return !StringUtils.hasText(content) || content.length() < 60;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private BigDecimal round(double value) {
        return BigDecimal.valueOf(value).setScale(6, RoundingMode.HALF_UP);
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.toLowerCase().replace('\n', ' ').replace('\r', ' ').replaceAll("\\s+", " ").trim();
    }

    private String firstText(Object... values) {
        if (values == null) {
            return null;
        }
        for (Object value : values) {
            if (value == null) {
                continue;
            }
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text)) {
                return text;
            }
        }
        return null;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }
}
