package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.entity.*;
import com.alikeyou.itmoduleai.enums.AiAnalysisMode;
import com.alikeyou.itmoduleai.enums.GroundingStatus;
import com.alikeyou.itmoduleai.provider.support.EmbeddingNameNormalizer;
import com.alikeyou.itmoduleai.repository.*;
import com.alikeyou.itmoduleai.service.KnowledgeEmbeddingService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.*;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class AiKnowledgeResolver {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5_.$#/-]+");
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);

    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final KnowledgeChunkEmbeddingRepository knowledgeChunkEmbeddingRepository;
    private final AiRetrievalLogRepository aiRetrievalLogRepository;
    private final AiCodeSymbolRepository aiCodeSymbolRepository;
    private final AiCodeReferenceRepository aiCodeReferenceRepository;
    private final KnowledgeEmbeddingService knowledgeEmbeddingService;
    private final CodeQueryIntentClassifier codeQueryIntentClassifier;
    private final CodeRetrievalPlanner codeRetrievalPlanner;
    private final CodeReranker codeReranker;
    private final ObjectMapper objectMapper;

    @Value("${ai.rag.default-top-k:5}")
    private int defaultTopK;
    @Value("${ai.rag.max-top-k:20}")
    private int maxTopK;
    @Value("${ai.rag.max-vector-candidates:5000}")
    private int maxVectorCandidates;
    @Value("${ai.rag.max-keyword-candidates:1200}")
    private int maxKeywordCandidates;

    public List<Long> resolveKnowledgeBaseIds(AiSession session, List<Long> requestKnowledgeBaseIds) {
        Set<Long> sessionIds = new LinkedHashSet<>();
        if (session != null && session.getDefaultKnowledgeBase() != null) {
            sessionIds.add(session.getDefaultKnowledgeBase().getId());
        }
        if (session != null && session.getId() != null) {
            for (AiSessionKnowledgeBase binding : aiSessionKnowledgeBaseRepository.findBySession_IdOrderByPriorityAscIdAsc(session.getId())) {
                if (binding.getKnowledgeBase() != null) {
                    sessionIds.add(binding.getKnowledgeBase().getId());
                }
            }
        }

        Set<Long> ids = new LinkedHashSet<>();
        if (requestKnowledgeBaseIds != null) {
            requestKnowledgeBaseIds.stream()
                    .filter(Objects::nonNull)
                    .filter(id -> sessionIds.isEmpty() || sessionIds.contains(id))
                    .forEach(ids::add);
        }
        ids.addAll(sessionIds);
        return new ArrayList<>(ids);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(AiSession session, String userQuestion, List<Long> requestKnowledgeBaseIds, Integer topK) {
        return retrieve(session, userQuestion, requestKnowledgeBaseIds, topK, AiAnalysisMode.DOC_QA,
                false, null, null, null);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(AiSession session,
                                    String userQuestion,
                                    List<Long> requestKnowledgeBaseIds,
                                    Integer topK,
                                    AiAnalysisMode requestedMode,
                                    Boolean strictGrounding,
                                    String entryFile,
                                    String symbolHint,
                                    Integer traceDepth) {
        return retrieve(session, userQuestion, requestKnowledgeBaseIds, topK, requestedMode,
                strictGrounding, entryFile, symbolHint, traceDepth, null);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(AiSession session,
                                    String userQuestion,
                                    List<Long> requestKnowledgeBaseIds,
                                    Integer topK,
                                    AiAnalysisMode requestedMode,
                                    Boolean strictGrounding,
                                    String entryFile,
                                    String symbolHint,
                                    Integer traceDepth,
                                    String actionCode) {
        List<Long> kbIds = resolveKnowledgeBaseIds(session, requestKnowledgeBaseIds);
        int limit = normalizeTopK(topK, session);
        if (!StringUtils.hasText(userQuestion) || kbIds.isEmpty()) {
            return RetrievalResult.empty(kbIds, limit);
        }
        if (limit <= 0) {
            log.info("RAG skipped because topK is zero. knowledgeBaseIds={}", kbIds);
            return RetrievalResult.empty(kbIds, limit);
        }

        String normalizedQuestion = normalize(userQuestion);
        List<String> tokens = tokenize(userQuestion);
        AiAnalysisMode mode = codeQueryIntentClassifier.classify(requestedMode, actionCode, userQuestion, symbolHint);
        CodeAnalysisPlan plan = codeRetrievalPlanner.buildPlan(mode, Boolean.TRUE.equals(strictGrounding), limit,
                traceDepth, entryFile, symbolHint, normalizedQuestion, tokens, keywordTerms(normalizedQuestion, tokens));

        RetrievalResult result = switch (plan.getMode()) {
            case CODE_LOCATE -> retrieveForCodeLocate(session, userQuestion, kbIds, plan);
            case CODE_LOGIC -> retrieveForCodeLogic(session, userQuestion, kbIds, plan);
            default -> retrieveForDocQa(session, userQuestion, kbIds, plan);
        };
        log.info("RAG plan mode={} strict={} refused={} reason={} phases={}",
                result.getMode(), result.isStrictGrounding(), result.isRefused(), result.getRefusalReason(),
                plan.getPhases().stream().map(CodeAnalysisPlan.PlanPhase::phase).toList());
        return result;
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieveForDocQa(AiSession session, String userQuestion, List<Long> kbIds, CodeAnalysisPlan plan) {
        HybridRecall recall = recallHybrid(session, userQuestion, kbIds, plan);
        List<KnowledgeRetrievalHit> ranked = diversifyAndRank(new ArrayList<>(recall.merged.values()), plan.getTopK()).stream()
                .peek(hit -> ensureTrace(hit, AiRetrievalLog.StageCode.RECALL, "hybrid_recall", AiRetrievalLog.CandidateSource.CHUNK,
                        "HYBRID_DOC_QA"))
                .toList();
        CodeEvidencePack pack = applyGrounding(plan, ranked, recall.stats, recall.degradeReason);
        logSummary(kbIds, recall.profile, recall.stats, recall.vectorHits.size(), recall.keywordHits.size(), ranked, recall.degradeReason);
        return toResult(kbIds, plan, recall, pack);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieveForCodeLocate(AiSession session, String userQuestion, List<Long> kbIds, CodeAnalysisPlan plan) {
        HybridRecall recall = recallHybrid(session, userQuestion, kbIds, plan);
        Map<Long, KnowledgeRetrievalHit> merged = new LinkedHashMap<>();
        mergeHits(merged, recallDeclarationFirst(kbIds, plan));
        mergeHits(merged, recall.keywordHits);
        if (merged.isEmpty()) {
            mergeHits(merged, recall.vectorHits);
        }
        if (merged.isEmpty()) {
            mergeHits(merged, recallFallback(kbIds, plan.getNormalizedQuestion(), plan.getTokens(), plan.getTopK()));
        }
        List<KnowledgeRetrievalHit> ranked = codeReranker.rerank(plan, new ArrayList<>(merged.values()), plan.getTopK());
        CodeEvidencePack pack = applyGrounding(plan, ranked, recall.stats, recall.degradeReason);
        logSummary(kbIds, recall.profile, recall.stats, recall.vectorHits.size(), recall.keywordHits.size(), ranked, recall.degradeReason);
        return toResult(kbIds, plan, recall.withMerged(merged), pack);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieveForCodeLogic(AiSession session, String userQuestion, List<Long> kbIds, CodeAnalysisPlan plan) {
        HybridRecall recall = recallHybrid(session, userQuestion, kbIds, plan);
        Map<Long, KnowledgeRetrievalHit> merged = new LinkedHashMap<>();

        List<KnowledgeRetrievalHit> declarationHits = recallDeclarationFirst(kbIds, plan);
        mergeHits(merged, declarationHits);

        List<KnowledgeRetrievalHit> graphHits = graphExpand(kbIds, declarationHits, plan);
        mergeHits(merged, graphHits);

        List<KnowledgeRetrievalHit> adjacentHits = adjacentChunkExpand(new ArrayList<>(merged.values()), plan);
        mergeHits(merged, adjacentHits);

        if (!plan.isStrictGrounding() || !declarationHits.isEmpty()) {
            mergeHits(merged, recall.keywordHits);
            if (merged.size() < plan.getTopK()) {
                mergeHits(merged, recall.vectorHits);
            }
        }

        List<KnowledgeRetrievalHit> ranked = codeReranker.rerank(plan, new ArrayList<>(merged.values()), plan.getTopK());
        CodeEvidencePack pack = applyGrounding(plan, ranked, recall.stats, recall.degradeReason);
        logSummary(kbIds, recall.profile, recall.stats, recall.vectorHits.size(), recall.keywordHits.size(), ranked, recall.degradeReason);
        return toResult(kbIds, plan, recall.withMerged(merged), pack);
    }

    private HybridRecall recallHybrid(AiSession session, String userQuestion, List<Long> kbIds, CodeAnalysisPlan plan) {
        String normalizedQuestion = plan.getNormalizedQuestion();
        List<String> tokens = plan.getTokens();
        EmbeddingProfile profile = resolveEmbeddingProfile(session, kbIds);
        EmbeddingStats stats = inspectEmbeddingStats(kbIds, profile);
        String degradeReason = null;

        Map<Long, KnowledgeRetrievalHit> merged = new LinkedHashMap<>();
        List<KnowledgeRetrievalHit> vectorHits = recallVector(kbIds, userQuestion, normalizedQuestion, tokens, profile, stats, plan.getTopK());
        mergeHits(merged, vectorHits);
        List<KnowledgeRetrievalHit> keywordHits = recallKeyword(kbIds, normalizedQuestion, tokens, plan.getTopK());
        mergeHits(merged, keywordHits);

        if (stats.queryVectorUnavailable()) {
            degradeReason = stats.queryVectorReason;
            log.warn("RAG vector recall degraded. knowledgeBaseIds={}, reason={}", kbIds, degradeReason);
        }
        if (merged.isEmpty()) {
            mergeHits(merged, recallFallback(kbIds, normalizedQuestion, tokens, plan.getTopK()));
            if (degradeReason == null) {
                degradeReason = "No vector or keyword candidates matched; used fallback candidates.";
            }
        }
        return new HybridRecall(profile, stats, degradeReason, merged, vectorHits, keywordHits);
    }

    public String buildKnowledgeAugmentedQuestion(String userQuestion, List<KnowledgeRetrievalHit> hits) {
        if (!StringUtils.hasText(userQuestion) || hits == null || hits.isEmpty()) {
            return userQuestion;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Answer using the evidence below as the primary source.\n");
        sb.append("If the evidence cannot fully confirm the answer, say naturally that the current knowledge base cannot fully confirm it.\n");
        sb.append("Do not invent facts, files, APIs, or behavior not supported by evidence.\n\n");
        sb.append("[Evidence]\n");
        Set<Long> seenChunks = new HashSet<>();
        Set<String> seenText = new HashSet<>();
        int rank = 1;
        EvidenceBlock previous = null;
        for (KnowledgeRetrievalHit hit : hits.stream().sorted(Comparator.comparing(KnowledgeRetrievalHit::getRankNo, Comparator.nullsLast(Integer::compareTo))).toList()) {
            if (hit.getChunkId() != null && !seenChunks.add(hit.getChunkId())) continue;
            String snippet = compact(firstText(hit.getSnippet(), hit.getChunkContent()), 900);
            String key = normalize(snippet);
            if (StringUtils.hasText(key) && !seenText.add(key)) continue;
            EvidenceBlock block = EvidenceBlock.from(hit, snippet, rank);
            if (previous != null && previous.canMerge(block)) {
                previous = previous.merge(block);
                continue;
            }
            if (previous != null) appendEvidence(sb, previous);
            previous = block;
            rank++;
        }
        if (previous != null) appendEvidence(sb, previous);
        sb.append("[User Question]\n").append(userQuestion).append("\n\n");
        sb.append("[Rules]\n1. Answer directly first.\n2. Use evidence labels for references.\n3. Be explicit when evidence is incomplete.\n");
        return sb.toString();
    }

    public List<AiCitationResponse> buildCitations(List<KnowledgeRetrievalHit> hits) {
        if (hits == null || hits.isEmpty()) return List.of();
        List<AiCitationResponse> result = new ArrayList<>();
        for (KnowledgeRetrievalHit hit : hits) {
            result.add(AiCitationResponse.builder()
                    .knowledgeBaseId(hit.getKnowledgeBaseId()).knowledgeBaseName(hit.getKnowledgeBaseName())
                    .documentId(hit.getDocumentId()).documentTitle(hit.getDocumentTitle())
                    .fileName(hit.getFileName()).archiveEntryPath(hit.getArchiveEntryPath()).path(hit.getPath())
                    .chunkId(hit.getChunkId()).chunkIndex(hit.getChunkIndex()).chunkTitle(buildChunkTitle(hit))
                    .snippet(hit.getSnippet()).score(hit.getScore()).keywordScore(hit.getKeywordScore()).vectorScore(hit.getVectorScore())
                    .rankNo(hit.getRankNo()).language(hit.getLanguage()).symbolName(hit.getSymbolName()).symbolType(hit.getSymbolType())
                    .startLine(hit.getStartLine()).endLine(hit.getEndLine()).sectionName(hit.getSectionName())
                    .build());
        }
        return result;
    }

    @Transactional
    public void saveRetrievalLogs(AiCallLog callLog, String queryText, List<KnowledgeRetrievalHit> hits) {
        if (callLog == null || callLog.getId() == null || hits == null || hits.isEmpty()) return;
        Instant now = Instant.now();
        List<AiRetrievalLog> logs = new ArrayList<>();
        for (KnowledgeRetrievalHit hit : hits) {
            AiRetrievalLog item = new AiRetrievalLog();
            item.setCallLog(callLog);
            item.setKnowledgeBase(hit.getKnowledgeBase());
            item.setDocument(hit.getDocument());
            item.setChunk(hit.getChunk());
            item.setQueryText(queryText);
            item.setScore(hit.getScore());
            item.setScoreKeyword(hit.getKeywordScore());
            item.setScoreVector(hit.getVectorScore());
            item.setScoreGraph(hit.getGraphScore());
            item.setScoreRerank(hit.getRerankScore());
            item.setRankNo(hit.getRankNo());
            item.setRetrievalMethod(hit.getRetrievalMethod() == null ? AiRetrievalLog.RetrievalMethod.HYBRID : hit.getRetrievalMethod());
            item.setStageCode(hit.getStageCode() == null ? AiRetrievalLog.StageCode.RECALL : hit.getStageCode());
            item.setPhase(hit.getPhase());
            item.setCandidateSource(hit.getCandidateSource());
            item.setSymbol(hit.getSymbol());
            item.setReference(hit.getReference());
            item.setHitReasonJson(hit.getHitReasonJson());
            item.setScoreDetailJson(hit.getScoreDetailJson());
            item.setCreatedAt(now);
            logs.add(item);
        }
        aiRetrievalLogRepository.saveAll(logs);
    }

    private RetrievalResult toResult(List<Long> kbIds, CodeAnalysisPlan plan, HybridRecall recall, CodeEvidencePack pack) {
        return new RetrievalResult(kbIds, pack.getHits(), plan.getTopK(), recall.vectorHits.size(), recall.keywordHits.size(),
                recall.stats.availableEmbeddingCount, recall.stats.providerFilteredCount, recall.stats.modelFilteredCount,
                recall.stats.statusFilteredCount, recall.degradeReason, plan.getMode(), plan.isStrictGrounding(),
                pack.getGroundingStatus(), pack.isRefused(), pack.getRefusalReason(), pack.getRefusalMessage());
    }

    private List<KnowledgeRetrievalHit> recallDeclarationFirst(List<Long> kbIds, CodeAnalysisPlan plan) {
        List<String> terms = new ArrayList<>(plan.getSymbolTerms());
        if (terms.isEmpty()) {
            terms.addAll(plan.getKeywordTerms());
        }
        if (terms.isEmpty()) {
            return List.of();
        }
        while (terms.size() < 5) terms.add(null);
        List<AiCodeSymbol> symbols = aiCodeSymbolRepository.findDeclarationCandidatesByKnowledgeBaseIds(
                kbIds, normalize(terms.get(0)), normalize(terms.get(1)), normalize(terms.get(2)),
                normalize(terms.get(3)), normalize(terms.get(4)),
                PageRequest.of(0, Math.min(maxKeywordCandidates, Math.max(plan.getTopK(), plan.getTopK() * 20))));
        if (symbols == null) {
            return List.of();
        }
        return symbols
                .stream()
                .map(symbol -> toDeclarationHit(symbol, plan))
                .filter(Objects::nonNull)
                .toList();
    }

    private KnowledgeRetrievalHit toDeclarationHit(AiCodeSymbol symbol, CodeAnalysisPlan plan) {
        if (symbol == null) {
            return null;
        }
        KnowledgeChunk chunk = symbol.getChunk();
        if (chunk == null && symbol.getId() != null) {
            chunk = knowledgeChunkRepository.findByPrimarySymbol_IdOrderByStartLineAsc(symbol.getId()).stream().findFirst().orElse(null);
        }
        if (chunk == null) {
            return null;
        }
        KnowledgeRetrievalHit hit = baseHit(chunk, plan.getNormalizedQuestion(), plan.getTokens());
        hit.setSymbol(symbol);
        hit.setSymbolName(firstText(symbol.getSymbolName(), hit.getSymbolName()));
        hit.setSymbolType(firstText(symbol.getSymbolKind(), hit.getSymbolType()));
        hit.setLanguage(firstText(symbol.getLanguage(), hit.getLanguage()));
        hit.setStartLine(symbol.getStartLine() == null ? hit.getStartLine() : symbol.getStartLine());
        hit.setEndLine(symbol.getEndLine() == null ? hit.getEndLine() : symbol.getEndLine());
        hit.setKeywordScore(max(scoreChunk(chunk, plan.getNormalizedQuestion(), plan.getTokens()), BigDecimal.valueOf(20D).setScale(6, RoundingMode.HALF_UP)));
        hit.setVectorScore(ZERO);
        hit.setGraphScore(ZERO);
        hit.setScore(hit.getKeywordScore());
        hit.setRetrievalMethod(AiRetrievalLog.RetrievalMethod.KEYWORD);
        setTrace(hit, AiRetrievalLog.StageCode.DECLARATION_FIRST, "declaration_first",
                AiRetrievalLog.CandidateSource.SYMBOL, "DECLARATION_SYMBOL_MATCH",
                details("symbolId", symbol.getId(), "symbolName", symbol.getSymbolName(), "qualifiedName", symbol.getQualifiedName()));
        return hit;
    }

    private List<KnowledgeRetrievalHit> graphExpand(List<Long> kbIds, List<KnowledgeRetrievalHit> declarationHits, CodeAnalysisPlan plan) {
        if (plan.getGraphDepth() <= 0 || declarationHits == null || declarationHits.isEmpty()) {
            return List.of();
        }
        Set<Long> frontier = new LinkedHashSet<>();
        for (KnowledgeRetrievalHit hit : declarationHits) {
            if (hit.getSymbol() != null && hit.getSymbol().getId() != null) {
                frontier.add(hit.getSymbol().getId());
            }
        }
        if (frontier.isEmpty()) {
            return List.of();
        }
        List<KnowledgeRetrievalHit> result = new ArrayList<>();
        Set<Long> visitedSymbols = new LinkedHashSet<>(frontier);
        for (int depth = 1; depth <= plan.getGraphDepth() && !frontier.isEmpty(); depth++) {
            List<AiCodeReference> refs = aiCodeReferenceRepository.findResolvedGraphEdgesBySymbolIds(kbIds, frontier);
            if (refs == null) {
                refs = List.of();
            }
            Set<Long> nextFrontier = new LinkedHashSet<>();
            for (AiCodeReference ref : refs) {
                addGraphHit(result, ref, ref.getFromChunk(), ref.getFromSymbol(), plan, depth, "FROM_REFERENCE");
                addGraphHit(result, ref, ref.getToChunk(), ref.getToSymbol(), plan, depth, "TO_DECLARATION");
                addSymbolToFrontier(nextFrontier, visitedSymbols, ref.getFromSymbol());
                addSymbolToFrontier(nextFrontier, visitedSymbols, ref.getToSymbol());
            }
            frontier = nextFrontier;
        }
        return result;
    }

    private void addGraphHit(List<KnowledgeRetrievalHit> result,
                             AiCodeReference reference,
                             KnowledgeChunk chunk,
                             AiCodeSymbol symbol,
                             CodeAnalysisPlan plan,
                             int depth,
                             String reason) {
        if (chunk == null) {
            return;
        }
        KnowledgeRetrievalHit hit = baseHit(chunk, plan.getNormalizedQuestion(), plan.getTokens());
        hit.setReference(reference);
        hit.setSymbol(symbol);
        if (symbol != null) {
            hit.setSymbolName(firstText(symbol.getSymbolName(), hit.getSymbolName()));
            hit.setSymbolType(firstText(symbol.getSymbolKind(), hit.getSymbolType()));
            hit.setStartLine(symbol.getStartLine() == null ? hit.getStartLine() : symbol.getStartLine());
            hit.setEndLine(symbol.getEndLine() == null ? hit.getEndLine() : symbol.getEndLine());
        }
        hit.setKeywordScore(scoreChunk(chunk, plan.getNormalizedQuestion(), plan.getTokens()));
        hit.setVectorScore(ZERO);
        hit.setGraphScore(BigDecimal.valueOf(Math.max(8D, 30D - depth * 6D)).setScale(6, RoundingMode.HALF_UP));
        hit.setScore(hit.getGraphScore().add(value(hit.getKeywordScore())).setScale(6, RoundingMode.HALF_UP));
        hit.setRetrievalMethod(AiRetrievalLog.RetrievalMethod.MANUAL);
        setTrace(hit, AiRetrievalLog.StageCode.GRAPH_EXPAND, "graph_expand",
                AiRetrievalLog.CandidateSource.REFERENCE, reason,
                details("referenceId", reference == null ? null : reference.getId(),
                        "refKind", reference == null ? null : reference.getRefKind(),
                        "depth", depth,
                        "symbolId", symbol == null ? null : symbol.getId()));
        result.add(hit);
    }

    private void addSymbolToFrontier(Set<Long> nextFrontier, Set<Long> visitedSymbols, AiCodeSymbol symbol) {
        if (symbol == null || symbol.getId() == null || visitedSymbols.contains(symbol.getId())) {
            return;
        }
        visitedSymbols.add(symbol.getId());
        nextFrontier.add(symbol.getId());
    }

    private List<KnowledgeRetrievalHit> adjacentChunkExpand(List<KnowledgeRetrievalHit> seeds, CodeAnalysisPlan plan) {
        if (seeds == null || seeds.isEmpty()) {
            return List.of();
        }
        Map<Long, KnowledgeRetrievalHit> result = new LinkedHashMap<>();
        Set<String> windows = new HashSet<>();
        for (KnowledgeRetrievalHit seed : seeds) {
            if (seed.getDocumentId() == null || seed.getChunkIndex() == null) {
                continue;
            }
            String windowKey = seed.getDocumentId() + ":" + seed.getChunkIndex();
            if (!windows.add(windowKey)) {
                continue;
            }
            List<KnowledgeChunk> chunks = knowledgeChunkRepository.findAdjacentCandidates(
                    seed.getDocumentId(), Math.max(0, seed.getChunkIndex() - 1), seed.getChunkIndex() + 1);
            if (chunks == null) {
                chunks = List.of();
            }
            for (KnowledgeChunk chunk : chunks) {
                if (chunk == null || chunk.getId() == null || Objects.equals(chunk.getId(), seed.getChunkId())) {
                    continue;
                }
                KnowledgeRetrievalHit hit = toKeywordHit(chunk, plan.getNormalizedQuestion(), plan.getTokens(),
                        AiRetrievalLog.RetrievalMethod.MANUAL);
                hit.setGraphScore(BigDecimal.valueOf(8D).setScale(6, RoundingMode.HALF_UP));
                hit.setScore(value(hit.getScore()).add(hit.getGraphScore()).setScale(6, RoundingMode.HALF_UP));
                setTrace(hit, AiRetrievalLog.StageCode.RECALL, "adjacent_chunk_expand",
                        AiRetrievalLog.CandidateSource.CHUNK, "ADJACENT_CHUNK_CONTEXT",
                        details("seedChunkId", seed.getChunkId(), "distance", Math.abs(chunk.getChunkIndex() - seed.getChunkIndex())));
                result.putIfAbsent(chunk.getId(), hit);
            }
        }
        return new ArrayList<>(result.values());
    }

    private CodeEvidencePack applyGrounding(CodeAnalysisPlan plan,
                                            List<KnowledgeRetrievalHit> hits,
                                            EmbeddingStats stats,
                                            String degradeReason) {
        List<KnowledgeRetrievalHit> evidence = hits == null ? List.of() : hits;
        if (!plan.isStrictGrounding()) {
            evidence.forEach(hit -> hit.setScoreDetailJson(ensureJson(hit.getScoreDetailJson())));
            return CodeEvidencePack.pass(plan, evidence, GroundingStatus.NOT_CHECKED);
        }
        boolean hasDeclaration = evidence.stream().anyMatch(this::isDeclarationEvidence);
        if (plan.isCodeLogic() && !hasDeclaration) {
            return markGroundingFailure(plan, evidence, "NO_DECLARATION_HIT");
        }
        if (evidence.isEmpty()) {
            return markGroundingFailure(plan, evidence, "NO_EVIDENCE");
        }
        boolean onlyPath = evidence.stream().allMatch(hit -> codeReranker.isPathOnly(plan, hit));
        if (onlyPath) {
            return markGroundingFailure(plan, evidence, "PATH_ONLY_HIT");
        }
        boolean embeddingDegraded = StringUtils.hasText(degradeReason) || (stats != null && stats.queryVectorUnavailable());
        if (embeddingDegraded && evidence.stream().filter(hit -> !codeReranker.isPathOnly(plan, hit)).count() < 2) {
            return markGroundingFailure(plan, evidence, "EMBEDDING_DEGRADED_INSUFFICIENT_EVIDENCE");
        }
        evidence.forEach(hit -> setGroundingTrace(hit, GroundingStatus.STRICT_PASS, null));
        return CodeEvidencePack.pass(plan, evidence, GroundingStatus.STRICT_PASS);
    }

    private CodeEvidencePack markGroundingFailure(CodeAnalysisPlan plan, List<KnowledgeRetrievalHit> hits, String reason) {
        hits.forEach(hit -> setGroundingTrace(hit, GroundingStatus.STRICT_FAIL, reason));
        return CodeEvidencePack.refuse(plan, hits, reason);
    }

    private boolean isDeclarationEvidence(KnowledgeRetrievalHit hit) {
        if (hit == null) {
            return false;
        }
        if (hit.getSymbol() != null && Boolean.TRUE.equals(hit.getSymbol().getIsDeclaration())) {
            return true;
        }
        return hit.getStageCode() == AiRetrievalLog.StageCode.DECLARATION_FIRST;
    }

    private void setTrace(KnowledgeRetrievalHit hit,
                          AiRetrievalLog.StageCode stageCode,
                          String phase,
                          AiRetrievalLog.CandidateSource candidateSource,
                          String reason,
                          Map<String, Object> detail) {
        if (hit == null) {
            return;
        }
        hit.setStageCode(stageCode);
        hit.setPhase(phase);
        hit.setCandidateSource(candidateSource);
        Map<String, Object> reasonJson = new LinkedHashMap<>();
        reasonJson.put("reason", reason);
        reasonJson.put("phase", phase);
        if (detail != null) {
            reasonJson.putAll(detail);
        }
        hit.setHitReasonJson(toJson(reasonJson));
        if (!StringUtils.hasText(hit.getScoreDetailJson())) {
            Map<String, Object> score = new LinkedHashMap<>();
            score.put("score", hit.getScore());
            score.put("keyword", hit.getKeywordScore());
            score.put("vector", hit.getVectorScore());
            score.put("graph", hit.getGraphScore());
            score.put("rerank", hit.getRerankScore());
            hit.setScoreDetailJson(toJson(score));
        }
    }

    private void ensureTrace(KnowledgeRetrievalHit hit,
                             AiRetrievalLog.StageCode stageCode,
                             String phase,
                             AiRetrievalLog.CandidateSource candidateSource,
                             String reason) {
        if (hit == null || StringUtils.hasText(hit.getPhase())) {
            return;
        }
        setTrace(hit, stageCode, phase, candidateSource, reason, Map.of());
    }

    private void setGroundingTrace(KnowledgeRetrievalHit hit, GroundingStatus status, String reason) {
        if (hit == null) {
            return;
        }
        Map<String, Object> score = readJson(hit.getScoreDetailJson());
        score.put("groundingStatus", status);
        if (StringUtils.hasText(reason)) {
            score.put("groundingReason", reason);
        }
        hit.setScoreDetailJson(toJson(score));
    }

    private Map<String, Object> details(Object... keysAndValues) {
        Map<String, Object> detail = new LinkedHashMap<>();
        if (keysAndValues == null) {
            return detail;
        }
        for (int i = 0; i + 1 < keysAndValues.length; i += 2) {
            Object key = keysAndValues[i];
            if (key != null) {
                detail.put(String.valueOf(key), keysAndValues[i + 1]);
            }
        }
        return detail;
    }

    private BigDecimal value(BigDecimal value) {
        return value == null ? ZERO : value;
    }

    private String ensureJson(String raw) {
        return StringUtils.hasText(raw) ? raw : "{}";
    }

    private Map<String, Object> readJson(String raw) {
        if (!StringUtils.hasText(raw)) {
            return new LinkedHashMap<>();
        }
        try {
            return new LinkedHashMap<>(objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {}));
        } catch (Exception ignored) {
            Map<String, Object> fallback = new LinkedHashMap<>();
            fallback.put("raw", raw);
            return fallback;
        }
    }

    private List<KnowledgeRetrievalHit> recallVector(List<Long> kbIds, String question, String normalizedQuestion,
                                                     List<String> tokens, EmbeddingProfile profile, EmbeddingStats stats, int topK) {
        if (profile == null || !profile.isComplete()) {
            stats.queryVectorReason = "Embedding provider/model is not configured.";
            return List.of();
        }
        Set<String> providerCandidates = EmbeddingNameNormalizer.providerLookupCandidates(profile.provider);
        Set<String> modelCandidates = EmbeddingNameNormalizer.modelLookupCandidates(profile.modelName);
        if (providerCandidates.isEmpty() || modelCandidates.isEmpty()) {
            stats.queryVectorReason = "Embedding provider/model is not configured.";
            return List.of();
        }
        List<Double> queryVector;
        try {
            queryVector = knowledgeEmbeddingService.embedText(question, profile.provider, profile.modelName, 768);
        } catch (Exception ex) {
            stats.queryVectorReason = "Query embedding failed: " + safeMessage(ex);
            return List.of();
        }
        if (queryVector == null || queryVector.isEmpty()) {
            stats.queryVectorReason = "Query embedding returned empty vector.";
            return List.of();
        }
        List<KnowledgeChunkEmbedding> embeddings = knowledgeChunkEmbeddingRepository.findLatestActiveByKnowledgeBaseIdsAndProviderAndModel(
                kbIds, providerCandidates, profile.provider, modelCandidates, profile.modelName,
                KnowledgeChunkEmbedding.Status.ACTIVE);
        if (embeddings == null) {
            return List.of();
        }
        return embeddings
                .stream()
                .map(e -> toVectorHit(e, queryVector, normalizedQuestion, tokens))
                .filter(Objects::nonNull)
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getVectorScore).reversed())
                .limit(Math.min(maxVectorCandidates, Math.max(topK, topK * 30L)))
                .toList();
    }

    private List<KnowledgeRetrievalHit> recallKeyword(List<Long> kbIds, String normalizedQuestion, List<String> tokens, int topK) {
        List<String> terms = new ArrayList<>(keywordTerms(normalizedQuestion, tokens));
        if (terms.isEmpty()) return List.of();
        while (terms.size() < 5) terms.add(null);
        List<KnowledgeChunk> chunks = knowledgeChunkRepository.findKeywordCandidatesByKnowledgeBaseIds(
                kbIds, terms.get(0), terms.get(1), terms.get(2), terms.get(3), terms.get(4),
                PageRequest.of(0, Math.min(maxKeywordCandidates, Math.max(topK, topK * 30))));
        if (chunks == null) {
            return List.of();
        }
        return chunks
                .stream()
                .map(chunk -> toKeywordHit(chunk, normalizedQuestion, tokens, AiRetrievalLog.RetrievalMethod.KEYWORD))
                .filter(hit -> positive(hit.getKeywordScore()))
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getKeywordScore).reversed()
                        .thenComparing(KnowledgeRetrievalHit::getDocumentId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(KnowledgeRetrievalHit::getChunkIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    private List<KnowledgeRetrievalHit> recallFallback(List<Long> kbIds, String normalizedQuestion, List<String> tokens, int topK) {
        int size = Math.min(maxKeywordCandidates, Math.max(topK, topK * 20));
        Map<Long, KnowledgeChunk> candidates = new LinkedHashMap<>();
        mergeCandidateChunks(candidates, knowledgeChunkRepository.findRecentCandidatesByKnowledgeBaseIds(kbIds, PageRequest.of(0, Math.min(size, 200))));
        mergeCandidateChunks(candidates, knowledgeChunkRepository.findDocumentOrderedCandidatesByKnowledgeBaseIds(kbIds, PageRequest.of(0, size)));
        mergeCandidateChunks(candidates, knowledgeChunkRepository.findFrontendPreferredCandidatesByKnowledgeBaseIds(kbIds, PageRequest.of(0, Math.min(size, 200))));
        return candidates.values().stream()
                .map(chunk -> toKeywordHit(chunk, normalizedQuestion, tokens, AiRetrievalLog.RetrievalMethod.MANUAL))
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getScore).reversed())
                .toList();
    }

    private KnowledgeRetrievalHit toVectorHit(KnowledgeChunkEmbedding embedding, List<Double> queryVector,
                                              String normalizedQuestion, List<String> tokens) {
        if (embedding == null || embedding.getChunk() == null) return null;
        List<Double> vector = knowledgeEmbeddingService.parseVectorPayload(embedding.getVectorPayload());
        double similarity = cosine(queryVector, vector);
        if (similarity <= 0D) return null;
        KnowledgeRetrievalHit hit = baseHit(embedding.getChunk(), normalizedQuestion, tokens);
        BigDecimal vectorScore = BigDecimal.valueOf(similarity * 100D).setScale(6, RoundingMode.HALF_UP);
        BigDecimal keywordScore = scoreChunk(embedding.getChunk(), normalizedQuestion, tokens);
        hit.setVectorScore(vectorScore);
        hit.setKeywordScore(keywordScore);
        hit.setGraphScore(ZERO);
        hit.setScore(mergeScore(keywordScore, vectorScore));
        hit.setRetrievalMethod(positive(keywordScore) ? AiRetrievalLog.RetrievalMethod.HYBRID : AiRetrievalLog.RetrievalMethod.VECTOR);
        setTrace(hit, AiRetrievalLog.StageCode.RECALL, "vector_recall", AiRetrievalLog.CandidateSource.CHUNK,
                positive(keywordScore) ? "VECTOR_AND_KEYWORD_MATCH" : "VECTOR_SIMILARITY_MATCH",
                details("similarity", vectorScore));
        return hit;
    }

    private KnowledgeRetrievalHit toKeywordHit(KnowledgeChunk chunk, String normalizedQuestion, List<String> tokens,
                                               AiRetrievalLog.RetrievalMethod method) {
        KnowledgeRetrievalHit hit = baseHit(chunk, normalizedQuestion, tokens);
        BigDecimal keywordScore = scoreChunk(chunk, normalizedQuestion, tokens);
        hit.setKeywordScore(keywordScore);
        hit.setVectorScore(ZERO);
        hit.setGraphScore(ZERO);
        hit.setScore(mergeScore(keywordScore, ZERO));
        hit.setRetrievalMethod(method);
        setTrace(hit, AiRetrievalLog.StageCode.RECALL,
                method == AiRetrievalLog.RetrievalMethod.MANUAL ? "fallback_recall" : "keyword_recall",
                AiRetrievalLog.CandidateSource.CHUNK,
                method == AiRetrievalLog.RetrievalMethod.MANUAL ? "FALLBACK_CANDIDATE" : "KEYWORD_CONTENT_OR_METADATA_MATCH",
                details("keywordScore", keywordScore));
        return hit;
    }

    private KnowledgeRetrievalHit baseHit(KnowledgeChunk chunk, String normalizedQuestion, List<String> tokens) {
        KnowledgeBase kb = chunk.getKnowledgeBase();
        KnowledgeDocument doc = chunk.getDocument();
        Map<String, Object> meta = metadata(chunk.getMetadataJson());
        String path = firstText(meta.get("path"), meta.get("archiveEntryPath"),
                chunk.getFilePath(),
                doc == null ? null : doc.getFilePath(),
                doc == null ? null : doc.getArchiveEntryPath(),
                doc == null ? null : doc.getSourceUrl(),
                doc == null ? null : doc.getFileName());
        AiCodeSymbol primarySymbol = chunk.getPrimarySymbol();
        return KnowledgeRetrievalHit.builder()
                .knowledgeBaseId(kb == null ? null : kb.getId()).knowledgeBaseName(kb == null ? null : kb.getName())
                .documentId(doc == null ? null : doc.getId()).documentTitle(doc == null ? null : doc.getTitle())
                .fileName(doc == null ? null : doc.getFileName()).archiveEntryPath(doc == null ? null : doc.getArchiveEntryPath())
                .path(path).chunkId(chunk.getId()).chunkIndex(chunk.getChunkIndex()).chunkContent(chunk.getContent())
                .snippet(buildSnippet(chunk.getContent(), normalizedQuestion, tokens, 420))
                .language(firstText(meta.get("language"), doc == null ? null : doc.getLanguage()))
                .symbolName(firstText(meta.get("symbolName"), primarySymbol == null ? null : primarySymbol.getSymbolName()))
                .symbolType(firstText(meta.get("symbolType"), primarySymbol == null ? null : primarySymbol.getSymbolKind()))
                .startLine(firstInt(toInt(meta.get("startLine")), chunk.getStartLine()))
                .endLine(firstInt(toInt(meta.get("endLine")), chunk.getEndLine()))
                .sectionName(firstText(meta.get("sectionName"), meta.get("title")))
                .knowledgeBase(kb).document(doc).chunk(chunk)
                .symbol(primarySymbol)
                .build();
    }

    private void mergeHits(Map<Long, KnowledgeRetrievalHit> target, List<KnowledgeRetrievalHit> source) {
        if (source == null) return;
        for (KnowledgeRetrievalHit hit : source) {
            if (hit == null || hit.getChunkId() == null) continue;
            KnowledgeRetrievalHit existing = target.get(hit.getChunkId());
            if (existing == null) {
                target.put(hit.getChunkId(), hit);
                continue;
            }
            BigDecimal keyword = max(existing.getKeywordScore(), hit.getKeywordScore());
            BigDecimal vector = max(existing.getVectorScore(), hit.getVectorScore());
            BigDecimal graph = max(existing.getGraphScore(), hit.getGraphScore());
            existing.setKeywordScore(keyword);
            existing.setVectorScore(vector);
            existing.setGraphScore(graph);
            existing.setScore(mergeScore(keyword, vector).add(graph).setScale(6, RoundingMode.HALF_UP));
            existing.setRetrievalMethod(positive(keyword) && positive(vector)
                    ? AiRetrievalLog.RetrievalMethod.HYBRID
                    : positive(vector) ? AiRetrievalLog.RetrievalMethod.VECTOR : hit.getRetrievalMethod());
            if (stagePriority(hit.getStageCode()) < stagePriority(existing.getStageCode())) {
                existing.setStageCode(hit.getStageCode());
                existing.setPhase(hit.getPhase());
                existing.setCandidateSource(hit.getCandidateSource());
                existing.setHitReasonJson(hit.getHitReasonJson());
                existing.setSymbol(hit.getSymbol());
                existing.setReference(hit.getReference());
            }
        }
    }

    private int stagePriority(AiRetrievalLog.StageCode stageCode) {
        if (stageCode == AiRetrievalLog.StageCode.DECLARATION_FIRST) return 0;
        if (stageCode == AiRetrievalLog.StageCode.GRAPH_EXPAND) return 1;
        if (stageCode == AiRetrievalLog.StageCode.RECALL) return 2;
        if (stageCode == AiRetrievalLog.StageCode.RERANK) return 3;
        if (stageCode == AiRetrievalLog.StageCode.GROUND) return 4;
        return 5;
    }

    private EmbeddingStats inspectEmbeddingStats(List<Long> kbIds, EmbeddingProfile profile) {
        EmbeddingStats stats = new EmbeddingStats();
        if (kbIds == null || kbIds.isEmpty()) {
            return stats;
        }
        long latestCount = knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIds(kbIds);
        long activeCount = knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndStatus(
                kbIds, KnowledgeChunkEmbedding.Status.ACTIVE);
        if (profile == null || !profile.isComplete()) {
            stats.availableEmbeddingCount = toCount(activeCount);
            stats.statusFilteredCount = toCount(Math.max(0L, latestCount - activeCount));
            return stats;
        }
        Set<String> providerCandidates = EmbeddingNameNormalizer.providerLookupCandidates(profile.provider);
        Set<String> modelCandidates = EmbeddingNameNormalizer.modelLookupCandidates(profile.modelName);
        long activeProviderCount = knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndStatus(
                kbIds, providerCandidates, profile.provider, KnowledgeChunkEmbedding.Status.ACTIVE);
        long activeProviderModelCount = knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatus(
                kbIds, providerCandidates, profile.provider, modelCandidates, profile.modelName, KnowledgeChunkEmbedding.Status.ACTIVE);
        long inactiveProviderModelCount = knowledgeChunkEmbeddingRepository.countLatestByKnowledgeBaseIdsAndProviderAndModelAndStatusNot(
                kbIds, providerCandidates, profile.provider, modelCandidates, profile.modelName, KnowledgeChunkEmbedding.Status.ACTIVE);
        stats.availableEmbeddingCount = toCount(activeProviderModelCount);
        stats.providerFilteredCount = toCount(Math.max(0L, activeCount - activeProviderCount));
        stats.modelFilteredCount = toCount(Math.max(0L, activeProviderCount - activeProviderModelCount));
        stats.statusFilteredCount = toCount(inactiveProviderModelCount);
        return stats;
    }

    private EmbeddingProfile resolveEmbeddingProfile(AiSession session, List<Long> kbIds) {
        if (session != null && session.getDefaultKnowledgeBase() != null) {
            EmbeddingProfile profile = EmbeddingProfile.from(session.getDefaultKnowledgeBase());
            if (profile.isComplete()) return profile;
        }
        for (Long id : kbIds) {
            KnowledgeBase kb = knowledgeBaseRepository.findById(id).orElse(null);
            EmbeddingProfile profile = EmbeddingProfile.from(kb);
            if (profile.isComplete()) return profile;
        }
        return EmbeddingProfile.empty();
    }

    private BigDecimal scoreChunk(KnowledgeChunk chunk, String normalizedQuestion, List<String> tokens) {
        if (chunk == null) return ZERO;
        KnowledgeDocument doc = chunk.getDocument();
        KnowledgeBase kb = chunk.getKnowledgeBase();
        Map<String, Object> meta = metadata(chunk.getMetadataJson());
        String content = normalize(chunk.getContent());
        String title = normalize(doc == null ? null : doc.getTitle());
        String fileName = normalize(doc == null ? null : doc.getFileName());
        String path = normalize(firstText(doc == null ? null : doc.getArchiveEntryPath(), doc == null ? null : doc.getSourceUrl()));
        String kbName = normalize(kb == null ? null : kb.getName());
        String symbol = normalize(firstText(meta.get("symbolName"), meta.get("sectionName"), meta.get("title")));
        double score = 0D;
        score += containsScore(content, normalizedQuestion, 10D);
        score += containsScore(title, normalizedQuestion, 7D);
        score += containsScore(fileName, normalizedQuestion, 5D);
        score += containsScore(path, normalizedQuestion, 5.5D);
        score += containsScore(kbName, normalizedQuestion, 3D);
        score += containsScore(symbol, normalizedQuestion, 6D);
        int matched = 0;
        for (String token : tokens) {
            int count = 0;
            count += countContains(content, token);
            count += countContains(title, token) * 3;
            count += countContains(fileName, token) * 3;
            count += countContains(path, token) * 3;
            count += countContains(kbName, token);
            count += countContains(symbol, token) * 4;
            if (count > 0) matched++;
            score += Math.min(count, 12) * 1.1D;
        }
        if (!tokens.isEmpty()) score += ((double) matched / (double) tokens.size()) * 6D;
        if (StringUtils.hasText(content) && content.length() <= 1000) score += 0.25D;
        return BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP);
    }

    private List<KnowledgeRetrievalHit> diversifyAndRank(List<KnowledgeRetrievalHit> hits, int limit) {
        if (hits == null || hits.isEmpty()) return List.of();
        List<KnowledgeRetrievalHit> sorted = hits.stream()
                .filter(hit -> positive(hit.getScore()))
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getScore).reversed()
                        .thenComparing(KnowledgeRetrievalHit::getVectorScore, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(KnowledgeRetrievalHit::getDocumentId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(KnowledgeRetrievalHit::getChunkIndex, Comparator.nullsLast(Integer::compareTo)))
                .toList();
        List<KnowledgeRetrievalHit> selected = new ArrayList<>();
        Set<Long> selectedChunkIds = new HashSet<>();
        Map<String, Integer> docCounts = new HashMap<>();
        fillSelected(sorted, selected, selectedChunkIds, docCounts, limit, 2);
        fillSelected(sorted, selected, selectedChunkIds, docCounts, limit, Integer.MAX_VALUE);
        List<KnowledgeRetrievalHit> result = new ArrayList<>();
        int rank = 1;
        for (KnowledgeRetrievalHit hit : selected) {
            result.add(copyWithRank(hit, rank++));
        }
        return result;
    }

    private void fillSelected(List<KnowledgeRetrievalHit> sorted, List<KnowledgeRetrievalHit> selected,
                              Set<Long> selectedChunkIds, Map<String, Integer> docCounts, int limit, int maxPerDoc) {
        for (KnowledgeRetrievalHit hit : sorted) {
            if (selected.size() >= limit) return;
            if (hit.getChunkId() != null && selectedChunkIds.contains(hit.getChunkId())) continue;
            String docKey = hit.getDocumentId() == null ? "C:" + hit.getChunkId() : "D:" + hit.getDocumentId();
            int count = docCounts.getOrDefault(docKey, 0);
            if (count >= maxPerDoc) continue;
            selected.add(hit);
            if (hit.getChunkId() != null) selectedChunkIds.add(hit.getChunkId());
            docCounts.put(docKey, count + 1);
        }
    }

    private KnowledgeRetrievalHit copyWithRank(KnowledgeRetrievalHit hit, int rankNo) {
        KnowledgeRetrievalHit copy = KnowledgeRetrievalHit.builder()
                .knowledgeBaseId(hit.getKnowledgeBaseId()).knowledgeBaseName(hit.getKnowledgeBaseName())
                .documentId(hit.getDocumentId()).documentTitle(hit.getDocumentTitle())
                .fileName(hit.getFileName()).archiveEntryPath(hit.getArchiveEntryPath()).path(hit.getPath())
                .chunkId(hit.getChunkId()).chunkIndex(hit.getChunkIndex()).chunkContent(hit.getChunkContent())
                .snippet(hit.getSnippet()).score(hit.getScore()).keywordScore(hit.getKeywordScore()).vectorScore(hit.getVectorScore())
                .graphScore(hit.getGraphScore()).rerankScore(hit.getRerankScore())
                .rankNo(rankNo).retrievalMethod(hit.getRetrievalMethod()).stageCode(hit.getStageCode())
                .candidateSource(hit.getCandidateSource()).phase(hit.getPhase())
                .hitReasonJson(hit.getHitReasonJson()).scoreDetailJson(hit.getScoreDetailJson())
                .language(hit.getLanguage()).symbolName(hit.getSymbolName()).symbolType(hit.getSymbolType())
                .startLine(hit.getStartLine()).endLine(hit.getEndLine()).sectionName(hit.getSectionName())
                .knowledgeBase(hit.getKnowledgeBase()).document(hit.getDocument()).chunk(hit.getChunk())
                .symbol(hit.getSymbol()).reference(hit.getReference())
                .build();
        return copy;
    }

    private void appendEvidence(StringBuilder sb, EvidenceBlock block) {
        sb.append("Evidence #").append(block.rankNo).append('\n');
        appendLine(sb, "knowledgeBase", block.knowledgeBaseName);
        appendLine(sb, "document", block.documentTitle);
        appendLine(sb, "path", block.path);
        appendLine(sb, "chunk", block.chunkLabel());
        appendLine(sb, "language", block.language);
        appendLine(sb, "section", block.sectionName);
        appendLine(sb, "symbol", block.symbolLabel());
        appendLine(sb, "lines", block.lineLabel());
        sb.append("snippet:\n").append(block.snippet).append("\n\n");
    }

    private void appendLine(StringBuilder sb, String key, String value) {
        if (StringUtils.hasText(value)) sb.append(key).append(": ").append(value).append('\n');
    }

    private void mergeCandidateChunks(Map<Long, KnowledgeChunk> target, List<KnowledgeChunk> source) {
        if (source == null) return;
        for (KnowledgeChunk chunk : source) {
            if (chunk != null && chunk.getId() != null) target.putIfAbsent(chunk.getId(), chunk);
        }
    }

    private void logSummary(List<Long> kbIds, EmbeddingProfile profile, EmbeddingStats stats,
                            int vectorCount, int keywordCount, List<KnowledgeRetrievalHit> ranked, String degradeReason) {
        boolean keywordDegraded = stats.queryVectorUnavailable() && keywordCount > 0;
        log.info("RAG summary kbIds={} provider={} model={} vectorCandidates={} keywordCandidates={} availableEmbeddings={} providerFiltered={} modelFiltered={} statusFiltered={} finalHits={} keywordDegraded={} degradeReason={}",
                kbIds, profile.provider, profile.modelName, vectorCount, keywordCount, stats.availableEmbeddingCount,
                stats.providerFilteredCount, stats.modelFilteredCount, stats.statusFilteredCount,
                ranked == null ? 0 : ranked.size(), keywordDegraded, degradeReason);
        if (ranked != null) {
            for (KnowledgeRetrievalHit hit : ranked) {
                log.info("RAG rank={} chunkId={} kbId={} docId={} chunkIndex={} keywordScore={} vectorScore={} finalScore={} method={} path={} symbol={}",
                        hit.getRankNo(), hit.getChunkId(), hit.getKnowledgeBaseId(), hit.getDocumentId(),
                        hit.getChunkIndex(), hit.getKeywordScore(), hit.getVectorScore(), hit.getScore(),
                        hit.getRetrievalMethod(), hit.getPath(), hit.getSymbolName());
            }
        }
    }

    private List<String> tokenize(String question) {
        String[] raw = TOKEN_SPLIT_PATTERN.split(question == null ? "" : question.trim());
        List<String> tokens = new ArrayList<>();
        for (String item : raw) {
            String token = normalize(item);
            if (!StringUtils.hasText(token)) continue;
            if (token.length() == 1 && token.codePoints().allMatch(Character::isDigit)) continue;
            tokens.add(token);
        }
        return tokens.stream().distinct().toList();
    }

    private List<String> keywordTerms(String normalizedQuestion, List<String> tokens) {
        List<String> terms = new ArrayList<>();
        if (StringUtils.hasText(normalizedQuestion) && normalizedQuestion.length() <= 80) terms.add(normalizedQuestion);
        tokens.stream()
                .filter(StringUtils::hasText)
                .filter(token -> token.length() >= 2 || token.codePoints().anyMatch(cp -> Character.UnicodeScript.of(cp) == Character.UnicodeScript.HAN))
                .sorted(Comparator.comparingInt(String::length).reversed())
                .forEach(terms::add);
        return terms.stream().distinct().limit(5).toList();
    }

    private int normalizeTopK(Integer topK, AiSession session) {
        Integer value = topK;
        if (value == null && session != null && session.getDefaultKnowledgeBase() != null) {
            value = session.getDefaultKnowledgeBase().getDefaultTopK();
        }
        if (value == null) value = defaultTopK;
        int upper = Math.max(0, maxTopK);
        return Math.min(Math.max(0, value), upper);
    }

    private int toCount(long value) {
        if (value <= 0L) return 0;
        return value > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) value;
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) return "";
        return text.toLowerCase(Locale.ROOT).replace('\n', ' ').replace('\r', ' ').replace('\t', ' ')
                .replaceAll("\\s+", " ").trim();
    }

    private String buildSnippet(String content, String normalizedQuestion, List<String> tokens, int maxLength) {
        if (!StringUtils.hasText(content)) return null;
        String source = content.trim();
        if (source.length() <= maxLength) return source;
        String lower = source.toLowerCase(Locale.ROOT);
        int index = StringUtils.hasText(normalizedQuestion) ? lower.indexOf(normalizedQuestion) : -1;
        if (index < 0) {
            for (String token : tokens) {
                index = lower.indexOf(token);
                if (index >= 0) break;
            }
        }
        if (index < 0) return source.substring(0, maxLength) + "...";
        int start = Math.max(0, index - maxLength / 3);
        int end = Math.min(source.length(), start + maxLength);
        if (end - start < maxLength && start > 0) start = Math.max(0, end - maxLength);
        return (start > 0 ? "..." : "") + source.substring(start, end) + (end < source.length() ? "..." : "");
    }

    private double cosine(List<Double> a, List<Double> b) {
        if (a == null || b == null || a.isEmpty() || b.isEmpty()) return 0D;
        int size = Math.min(a.size(), b.size());
        double dot = 0D;
        double normA = 0D;
        double normB = 0D;
        for (int i = 0; i < size; i++) {
            double left = a.get(i) == null ? 0D : a.get(i);
            double right = b.get(i) == null ? 0D : b.get(i);
            dot += left * right;
            normA += left * left;
            normB += right * right;
        }
        return normA <= 0D || normB <= 0D ? 0D : dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private BigDecimal mergeScore(BigDecimal keyword, BigDecimal vector) {
        BigDecimal k = keyword == null ? ZERO : keyword;
        BigDecimal v = vector == null ? ZERO : vector;
        return v.multiply(BigDecimal.valueOf(0.72D)).add(k.multiply(BigDecimal.valueOf(0.28D))).setScale(6, RoundingMode.HALF_UP);
    }

    private BigDecimal max(BigDecimal a, BigDecimal b) {
        return (a == null ? ZERO : a).max(b == null ? ZERO : b).setScale(6, RoundingMode.HALF_UP);
    }

    private boolean positive(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    private int countContains(String source, String token) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(token)) return 0;
        int count = 0;
        int from = 0;
        while (true) {
            int found = source.indexOf(token, from);
            if (found < 0) return count;
            count++;
            from = found + token.length();
        }
    }

    private double containsScore(String source, String token, double weight) {
        return StringUtils.hasText(source) && StringUtils.hasText(token) && source.contains(token) ? weight : 0D;
    }

    private Map<String, Object> metadata(String raw) {
        if (!StringUtils.hasText(raw)) return Map.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ignored) {
            return Map.of();
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            return String.valueOf(value);
        }
    }

    private String firstText(Object... values) {
        if (values == null) return null;
        for (Object value : values) {
            if (value == null) continue;
            String text = String.valueOf(value).trim();
            if (StringUtils.hasText(text) && !"null".equalsIgnoreCase(text)) return text;
        }
        return null;
    }

    private Integer toInt(Object value) {
        if (value == null) return null;
        if (value instanceof Number number) return number.intValue();
        try {
            return Integer.parseInt(String.valueOf(value).trim());
        } catch (Exception ignored) {
            return null;
        }
    }

    private Integer firstInt(Integer... values) {
        if (values == null) {
            return null;
        }
        for (Integer value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String compact(String text, int maxLength) {
        if (!StringUtils.hasText(text)) return "";
        String source = text.trim();
        if (source.length() <= maxLength) return source;
        int end = source.indexOf('\n', maxLength / 2);
        if (end < 0 || end > maxLength) end = maxLength;
        return source.substring(0, end).trim() + "\n...";
    }

    private String buildChunkTitle(KnowledgeRetrievalHit hit) {
        if (hit.getDocumentTitle() == null) return hit.getChunkIndex() == null ? null : "Chunk " + hit.getChunkIndex();
        return hit.getChunkIndex() == null ? hit.getDocumentTitle() : hit.getDocumentTitle() + " #Chunk " + hit.getChunkIndex();
    }

    private String safeMessage(Exception ex) {
        return ex == null ? "unknown" : (StringUtils.hasText(ex.getMessage()) ? ex.getMessage() : ex.getClass().getSimpleName());
    }

    private record EmbeddingProfile(String provider, String modelName) {
        static EmbeddingProfile empty() {
            return new EmbeddingProfile(null, null);
        }

        static EmbeddingProfile from(KnowledgeBase kb) {
            if (kb == null) return empty();
            return new EmbeddingProfile(
                    EmbeddingNameNormalizer.normalizeProvider(kb.getEmbeddingProvider()),
                    EmbeddingNameNormalizer.normalizeModel(kb.getEmbeddingModel())
            );
        }

        boolean isComplete() {
            return StringUtils.hasText(provider) && StringUtils.hasText(modelName);
        }
    }

    private static class EmbeddingStats {
        private int availableEmbeddingCount;
        private int providerFilteredCount;
        private int modelFilteredCount;
        private int statusFilteredCount;
        private String queryVectorReason;

        boolean queryVectorUnavailable() {
            return StringUtils.hasText(queryVectorReason);
        }
    }

    private record HybridRecall(
            EmbeddingProfile profile,
            EmbeddingStats stats,
            String degradeReason,
            Map<Long, KnowledgeRetrievalHit> merged,
            List<KnowledgeRetrievalHit> vectorHits,
            List<KnowledgeRetrievalHit> keywordHits
    ) {
        HybridRecall withMerged(Map<Long, KnowledgeRetrievalHit> merged) {
            return new HybridRecall(profile, stats, degradeReason,
                    merged == null ? Map.of() : merged, vectorHits, keywordHits);
        }
    }

    @Getter
    public static class RetrievalResult {
        private final List<Long> knowledgeBaseIds;
        private final List<KnowledgeRetrievalHit> hits;
        private final Integer topK;
        private final Integer vectorCandidateCount;
        private final Integer keywordCandidateCount;
        private final Integer availableEmbeddingCount;
        private final Integer providerFilteredEmbeddingCount;
        private final Integer modelFilteredEmbeddingCount;
        private final Integer statusFilteredEmbeddingCount;
        private final String degradeReason;
        private final AiAnalysisMode mode;
        private final boolean strictGrounding;
        private final GroundingStatus groundingStatus;
        private final boolean refused;
        private final String refusalReason;
        private final String refusalMessage;

        public RetrievalResult(List<Long> knowledgeBaseIds, List<KnowledgeRetrievalHit> hits, Integer topK,
                               Integer vectorCandidateCount, Integer keywordCandidateCount,
                               Integer availableEmbeddingCount, Integer providerFilteredEmbeddingCount,
                               Integer modelFilteredEmbeddingCount, Integer statusFilteredEmbeddingCount,
                               String degradeReason) {
            this(knowledgeBaseIds, hits, topK, vectorCandidateCount, keywordCandidateCount,
                    availableEmbeddingCount, providerFilteredEmbeddingCount, modelFilteredEmbeddingCount,
                    statusFilteredEmbeddingCount, degradeReason, AiAnalysisMode.DOC_QA, false,
                    GroundingStatus.NOT_CHECKED, false, null, null);
        }

        public RetrievalResult(List<Long> knowledgeBaseIds, List<KnowledgeRetrievalHit> hits, Integer topK,
                               Integer vectorCandidateCount, Integer keywordCandidateCount,
                               Integer availableEmbeddingCount, Integer providerFilteredEmbeddingCount,
                               Integer modelFilteredEmbeddingCount, Integer statusFilteredEmbeddingCount,
                               String degradeReason, AiAnalysisMode mode, boolean strictGrounding,
                               GroundingStatus groundingStatus, boolean refused,
                               String refusalReason, String refusalMessage) {
            this.knowledgeBaseIds = knowledgeBaseIds == null ? List.of() : knowledgeBaseIds;
            this.hits = hits == null ? List.of() : hits;
            this.topK = topK;
            this.vectorCandidateCount = vectorCandidateCount;
            this.keywordCandidateCount = keywordCandidateCount;
            this.availableEmbeddingCount = availableEmbeddingCount;
            this.providerFilteredEmbeddingCount = providerFilteredEmbeddingCount;
            this.modelFilteredEmbeddingCount = modelFilteredEmbeddingCount;
            this.statusFilteredEmbeddingCount = statusFilteredEmbeddingCount;
            this.degradeReason = degradeReason;
            this.mode = mode == null ? AiAnalysisMode.DOC_QA : mode;
            this.strictGrounding = strictGrounding;
            this.groundingStatus = groundingStatus == null ? GroundingStatus.NOT_CHECKED : groundingStatus;
            this.refused = refused;
            this.refusalReason = refusalReason;
            this.refusalMessage = refusalMessage;
        }

        static RetrievalResult empty(List<Long> knowledgeBaseIds, Integer topK) {
            return new RetrievalResult(knowledgeBaseIds, List.of(), topK, 0, 0, 0, 0, 0, 0, null);
        }

        public boolean hasHits() {
            return !hits.isEmpty();
        }
    }

    private static class EvidenceBlock {
        private final int rankNo;
        private final Long documentId;
        private final Integer firstChunkIndex;
        private final Integer lastChunkIndex;
        private final String knowledgeBaseName;
        private final String documentTitle;
        private final String path;
        private final String language;
        private final String sectionName;
        private final String symbolName;
        private final String symbolType;
        private final Integer startLine;
        private final Integer endLine;
        private final String snippet;

        private EvidenceBlock(int rankNo, Long documentId, Integer firstChunkIndex, Integer lastChunkIndex,
                              String knowledgeBaseName, String documentTitle, String path, String language,
                              String sectionName, String symbolName, String symbolType, Integer startLine,
                              Integer endLine, String snippet) {
            this.rankNo = rankNo;
            this.documentId = documentId;
            this.firstChunkIndex = firstChunkIndex;
            this.lastChunkIndex = lastChunkIndex;
            this.knowledgeBaseName = knowledgeBaseName;
            this.documentTitle = documentTitle;
            this.path = path;
            this.language = language;
            this.sectionName = sectionName;
            this.symbolName = symbolName;
            this.symbolType = symbolType;
            this.startLine = startLine;
            this.endLine = endLine;
            this.snippet = snippet;
        }

        static EvidenceBlock from(KnowledgeRetrievalHit hit, String snippet, int rankNo) {
            return new EvidenceBlock(rankNo, hit.getDocumentId(), hit.getChunkIndex(), hit.getChunkIndex(),
                    hit.getKnowledgeBaseName(), hit.getDocumentTitle(), hit.getPath(), hit.getLanguage(),
                    hit.getSectionName(), hit.getSymbolName(), hit.getSymbolType(),
                    hit.getStartLine(), hit.getEndLine(), snippet);
        }

        boolean canMerge(EvidenceBlock other) {
            return other != null && Objects.equals(documentId, other.documentId)
                    && lastChunkIndex != null && other.firstChunkIndex != null
                    && other.firstChunkIndex == lastChunkIndex + 1 && Objects.equals(path, other.path);
        }

        EvidenceBlock merge(EvidenceBlock other) {
            String merged = (snippet == null ? "" : snippet) + "\n--- adjacent chunk ---\n" + (other.snippet == null ? "" : other.snippet);
            if (merged.length() > 1200) merged = merged.substring(0, 1200).trim() + "\n...";
            return new EvidenceBlock(rankNo, documentId, firstChunkIndex, other.lastChunkIndex, knowledgeBaseName,
                    documentTitle, path, language, sectionName, symbolName, symbolType, startLine,
                    other.endLine == null ? endLine : other.endLine, merged);
        }

        String chunkLabel() {
            if (firstChunkIndex == null) return null;
            return Objects.equals(firstChunkIndex, lastChunkIndex) ? "#" + firstChunkIndex : "#" + firstChunkIndex + "-#" + lastChunkIndex;
        }

        String symbolLabel() {
            if (!StringUtils.hasText(symbolName) && !StringUtils.hasText(symbolType)) return null;
            if (!StringUtils.hasText(symbolType)) return symbolName;
            if (!StringUtils.hasText(symbolName)) return symbolType;
            return symbolType + " " + symbolName;
        }

        String lineLabel() {
            if (startLine == null && endLine == null) return null;
            if (startLine != null && endLine != null) return startLine + "-" + endLine;
            return String.valueOf(startLine == null ? endLine : startLine);
        }
    }
}
