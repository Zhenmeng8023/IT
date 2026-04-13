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
import java.util.List;
import java.util.Map;
import java.util.Objects;

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
            if (positive(hit.getRerankScore()) || (plan != null && plan.isStrictGrounding() && isPathOnly(plan, hit))) {
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
        BigDecimal keyword = value(hit.getKeywordScore());
        BigDecimal vector = value(hit.getVectorScore());
        BigDecimal graph = value(hit.getGraphScore());
        double declarationBoost = declarationPriority(hit) ? 60D : 0D;
        double graphBoost = graph.doubleValue();
        double symbolBoost = symbolMatches(plan, hit) ? 18D : 0D;
        double contentBoost = contentMatches(plan, hit) ? 8D : 0D;
        double pathOnlyPenalty = isPathOnly(plan, hit) ? -35D : 0D;
        double adjacentBoost = Objects.equals(hit.getPhase(), "adjacent_chunk_expand") ? 8D : 0D;
        double genericScore = vector.doubleValue() * 0.46D + keyword.doubleValue() * 0.30D;
        double rerank = genericScore + declarationBoost + graphBoost + symbolBoost + contentBoost + adjacentBoost + pathOnlyPenalty;
        BigDecimal rerankScore = BigDecimal.valueOf(Math.max(0D, rerank)).setScale(6, RoundingMode.HALF_UP);
        hit.setRerankScore(rerankScore);
        hit.setScore(rerankScore);
        hit.setScoreDetailJson(toJson(scoreDetail(keyword, vector, graph, declarationBoost, symbolBoost,
                contentBoost, adjacentBoost, pathOnlyPenalty, rerankScore)));
    }

    private Map<String, Object> scoreDetail(BigDecimal keyword,
                                            BigDecimal vector,
                                            BigDecimal graph,
                                            double declarationBoost,
                                            double symbolBoost,
                                            double contentBoost,
                                            double adjacentBoost,
                                            double pathOnlyPenalty,
                                            BigDecimal rerankScore) {
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("keyword", keyword);
        detail.put("vector", vector);
        detail.put("graph", graph);
        detail.put("declarationBoost", round(declarationBoost));
        detail.put("symbolBoost", round(symbolBoost));
        detail.put("contentBoost", round(contentBoost));
        detail.put("adjacentBoost", round(adjacentBoost));
        detail.put("pathOnlyPenalty", round(pathOnlyPenalty));
        detail.put("rerank", rerankScore);
        return detail;
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
