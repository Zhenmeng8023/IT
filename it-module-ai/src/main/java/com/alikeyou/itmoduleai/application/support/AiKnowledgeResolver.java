package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeChunk;
import com.alikeyou.itmoduleai.repository.AiRetrievalLogRepository;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeChunkRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiKnowledgeResolver {

    private static final Pattern TOKEN_SPLIT_PATTERN =
            Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+");

    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AiRetrievalLogRepository aiRetrievalLogRepository;

    public List<Long> resolveKnowledgeBaseIds(AiSession session, List<Long> requestKnowledgeBaseIds) {
        if (requestKnowledgeBaseIds != null && !requestKnowledgeBaseIds.isEmpty()) {
            return requestKnowledgeBaseIds.stream().distinct().toList();
        }

        Set<Long> ids = new LinkedHashSet<>();

        if (session != null
                && session.getDefaultKnowledgeBase() != null
                && session.getDefaultKnowledgeBase().getId() != null) {
            ids.add(session.getDefaultKnowledgeBase().getId());
        }

        if (session == null || session.getId() == null) {
            return new ArrayList<>(ids);
        }

        List<AiSessionKnowledgeBase> bindings =
                aiSessionKnowledgeBaseRepository.findBySession_IdOrderByPriorityAscIdAsc(session.getId());

        for (AiSessionKnowledgeBase item : bindings) {
            if (item.getKnowledgeBase() != null && item.getKnowledgeBase().getId() != null) {
                ids.add(item.getKnowledgeBase().getId());
            }
        }

        return new ArrayList<>(ids);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(
            AiSession session,
            String userQuestion,
            List<Long> requestKnowledgeBaseIds,
            Integer topK
    ) {
        List<Long> knowledgeBaseIds = resolveKnowledgeBaseIds(session, requestKnowledgeBaseIds);
        if (!StringUtils.hasText(userQuestion) || knowledgeBaseIds.isEmpty()) {
            return new RetrievalResult(knowledgeBaseIds, List.of());
        }

        List<String> tokens = tokenize(userQuestion);
        if (tokens.isEmpty()) {
            return new RetrievalResult(knowledgeBaseIds, List.of());
        }

        int safeTopK = Math.max(topK == null ? 0 : topK, 5);
        int candidateSize = Math.min(Math.max(safeTopK * 30, 80), 800);

        List<KnowledgeChunk> candidates = loadCandidates(knowledgeBaseIds, candidateSize);
        if (candidates.isEmpty()) {
            return new RetrievalResult(knowledgeBaseIds, List.of());
        }

        String normalizedQuestion = normalize(userQuestion);
        List<KnowledgeRetrievalHit> hits = new ArrayList<>();

        for (KnowledgeChunk chunk : candidates) {
            BigDecimal score = scoreChunk(chunk, normalizedQuestion, tokens);
            if (score.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            hits.add(KnowledgeRetrievalHit.builder()
                    .knowledgeBaseId(chunk.getKnowledgeBase() == null ? null : chunk.getKnowledgeBase().getId())
                    .knowledgeBaseName(chunk.getKnowledgeBase() == null ? null : chunk.getKnowledgeBase().getName())
                    .documentId(chunk.getDocument() == null ? null : chunk.getDocument().getId())
                    .documentTitle(chunk.getDocument() == null ? null : chunk.getDocument().getTitle())
                    .chunkId(chunk.getId())
                    .chunkIndex(chunk.getChunkIndex())
                    .chunkContent(chunk.getContent())
                    .snippet(buildSnippet(chunk.getContent(), normalizedQuestion, tokens, 220))
                    .score(score)
                    .knowledgeBase(chunk.getKnowledgeBase())
                    .document(chunk.getDocument())
                    .chunk(chunk)
                    .build());
        }

        List<KnowledgeRetrievalHit> ranked = diversifyAndRank(hits, safeTopK);
        return new RetrievalResult(knowledgeBaseIds, ranked);
    }

    public String buildKnowledgeAugmentedQuestion(String userQuestion, List<KnowledgeRetrievalHit> hits) {
        if (!StringUtils.hasText(userQuestion) || hits == null || hits.isEmpty()) {
            return userQuestion;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("请优先基于下面提供的知识片段回答用户问题。\n");
        sb.append("如果知识片段不足以支持结论，请明确说明“根据当前知识库无法完全确认”，不要捏造。\n\n");
        sb.append("〖知识片段〗\n");

        for (KnowledgeRetrievalHit hit : hits) {
            sb.append("[")
                    .append(hit.getRankNo())
                    .append("] ")
                    .append(hit.getDocumentTitle() == null ? "未命名文档" : hit.getDocumentTitle());

            if (hit.getChunkIndex() != null) {
                sb.append(" #块").append(hit.getChunkIndex());
            }

            sb.append("\n");
            sb.append(hit.getChunkContent()).append("\n\n");
        }

        sb.append("〖用户问题〗\n").append(userQuestion).append("\n\n");
        sb.append("回答要求：\n");
        sb.append("1. 先直接回答问题。\n");
        sb.append("2. 回答尽量基于知识片段，不要虚构未出现的事实。\n");
        sb.append("3. 如果引用了知识片段，不要照抄整段内容。\n");

        return sb.toString();
    }

    public List<AiCitationResponse> buildCitations(List<KnowledgeRetrievalHit> hits) {
        List<AiCitationResponse> result = new ArrayList<>();
        if (hits == null || hits.isEmpty()) {
            return result;
        }

        for (KnowledgeRetrievalHit hit : hits) {
            result.add(AiCitationResponse.builder()
                    .knowledgeBaseId(hit.getKnowledgeBaseId())
                    .knowledgeBaseName(hit.getKnowledgeBaseName())
                    .documentId(hit.getDocumentId())
                    .documentTitle(hit.getDocumentTitle())
                    .chunkId(hit.getChunkId())
                    .chunkIndex(hit.getChunkIndex())
                    .chunkTitle(buildChunkTitle(hit))
                    .snippet(hit.getSnippet())
                    .score(hit.getScore())
                    .rankNo(hit.getRankNo())
                    .build());
        }

        return result;
    }

    @Transactional
    public void saveRetrievalLogs(AiCallLog callLog, String queryText, List<KnowledgeRetrievalHit> hits) {
        if (callLog == null || callLog.getId() == null || hits == null || hits.isEmpty()) {
            return;
        }

        List<AiRetrievalLog> logs = new ArrayList<>();
        Instant now = Instant.now();

        for (KnowledgeRetrievalHit hit : hits) {
            AiRetrievalLog log = new AiRetrievalLog();
            log.setCallLog(callLog);
            log.setKnowledgeBase(hit.getKnowledgeBase());
            log.setDocument(hit.getDocument());
            log.setChunk(hit.getChunk());
            log.setQueryText(queryText);
            log.setScore(hit.getScore());
            log.setRankNo(hit.getRankNo());
            log.setRetrievalMethod(AiRetrievalLog.RetrievalMethod.KEYWORD);
            log.setCreatedAt(now);
            logs.add(log);
        }

        aiRetrievalLogRepository.saveAll(logs);
    }

    private List<KnowledgeChunk> loadCandidates(List<Long> knowledgeBaseIds, int candidateSize) {
        int recentSize = Math.max(30, candidateSize / 2);

        Map<Long, KnowledgeChunk> merged = new LinkedHashMap<>();

        mergeCandidates(
                merged,
                knowledgeChunkRepository.findDocumentOrderedCandidatesByKnowledgeBaseIds(
                        knowledgeBaseIds,
                        PageRequest.of(0, candidateSize)
                )
        );

        mergeCandidates(
                merged,
                knowledgeChunkRepository.findRecentCandidatesByKnowledgeBaseIds(
                        knowledgeBaseIds,
                        PageRequest.of(0, recentSize)
                )
        );

        return new ArrayList<>(merged.values());
    }

    private void mergeCandidates(Map<Long, KnowledgeChunk> target, List<KnowledgeChunk> source) {
        if (source == null || source.isEmpty()) {
            return;
        }

        for (KnowledgeChunk chunk : source) {
            if (chunk == null || chunk.getId() == null) {
                continue;
            }
            target.putIfAbsent(chunk.getId(), chunk);
        }
    }

    private List<String> tokenize(String question) {
        String[] raw = TOKEN_SPLIT_PATTERN.split(question == null ? "" : question.trim());
        List<String> tokens = new ArrayList<>();

        for (String item : raw) {
            if (!StringUtils.hasText(item)) {
                continue;
            }

            String normalized = normalize(item);
            if (!StringUtils.hasText(normalized)) {
                continue;
            }

            if (normalized.length() == 1 && normalized.codePoints().allMatch(Character::isDigit)) {
                continue;
            }

            tokens.add(normalized);
        }

        return tokens.stream().distinct().toList();
    }

    private BigDecimal scoreChunk(KnowledgeChunk chunk, String normalizedQuestion, List<String> tokens) {
        String content = normalize(chunk.getContent());
        String title = normalize(chunk.getDocument() == null ? null : chunk.getDocument().getTitle());
        String fileName = normalize(chunk.getDocument() == null ? null : chunk.getDocument().getFileName());
        String kbName = normalize(chunk.getKnowledgeBase() == null ? null : chunk.getKnowledgeBase().getName());

        if (!StringUtils.hasText(content)
                && !StringUtils.hasText(title)
                && !StringUtils.hasText(fileName)
                && !StringUtils.hasText(kbName)) {
            return BigDecimal.ZERO.setScale(6, RoundingMode.HALF_UP);
        }

        double score = 0D;

        if (StringUtils.hasText(normalizedQuestion)) {
            if (StringUtils.hasText(content) && content.contains(normalizedQuestion)) {
                score += 10D;
            }
            if (StringUtils.hasText(title) && title.contains(normalizedQuestion)) {
                score += 7D;
            }
            if (StringUtils.hasText(fileName) && fileName.contains(normalizedQuestion)) {
                score += 5D;
            }
            if (StringUtils.hasText(kbName) && kbName.contains(normalizedQuestion)) {
                score += 3D;
            }
        }

        int matchedTokenCount = 0;
        double tokenScore = 0D;

        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }

            int contentCount = countContains(content, token);
            int titleCount = countContains(title, token);
            int fileNameCount = countContains(fileName, token);
            int kbCount = countContains(kbName, token);

            if (contentCount > 0 || titleCount > 0 || fileNameCount > 0 || kbCount > 0) {
                matchedTokenCount++;
            }

            tokenScore += Math.min(contentCount, 8) * 1.1D;
            tokenScore += Math.min(titleCount, 4) * 2.6D;
            tokenScore += Math.min(fileNameCount, 4) * 2.4D;
            tokenScore += Math.min(kbCount, 2) * 1.4D;
        }

        score += tokenScore;

        if (!tokens.isEmpty()) {
            score += ((double) matchedTokenCount / (double) tokens.size()) * 6D;
        }

        score += Math.min(countBigramMatches(content, title, fileName, tokens), 4) * 1.8D;

        if (StringUtils.hasText(content) && content.length() <= 800) {
            score += 0.25D;
        }

        if (chunk.getChunkIndex() != null && chunk.getChunkIndex() == 0) {
            score += 0.2D;
        }

        return BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP);
    }

    private int countBigramMatches(String content, String title, String fileName, List<String> tokens) {
        if (tokens == null || tokens.size() < 2) {
            return 0;
        }

        int count = 0;
        for (int i = 0; i < tokens.size() - 1; i++) {
            String a = tokens.get(i);
            String b = tokens.get(i + 1);

            if (!StringUtils.hasText(a) || !StringUtils.hasText(b)) {
                continue;
            }

            String withSpace = a + " " + b;
            String noSpace = a + b;

            if (containsPhrase(content, withSpace, noSpace)
                    || containsPhrase(title, withSpace, noSpace)
                    || containsPhrase(fileName, withSpace, noSpace)) {
                count++;
            }
        }

        return count;
    }

    private boolean containsPhrase(String source, String withSpace, String noSpace) {
        if (!StringUtils.hasText(source)) {
            return false;
        }
        return source.contains(withSpace) || source.contains(noSpace);
    }

    private int countContains(String source, String token) {
        if (!StringUtils.hasText(source) || !StringUtils.hasText(token)) {
            return 0;
        }

        int fromIndex = 0;
        int count = 0;

        while (true) {
            int found = source.indexOf(token, fromIndex);
            if (found < 0) {
                break;
            }
            count++;
            fromIndex = found + token.length();
        }

        return count;
    }

    private String normalize(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }

        return text.toLowerCase(Locale.ROOT)
                .replace('\n', ' ')
                .replace('\r', ' ')
                .replace('\t', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String buildSnippet(String content, String normalizedQuestion, List<String> tokens, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return null;
        }

        String source = content.trim();
        if (source.length() <= maxLength) {
            return source;
        }

        String lowerSource = source.toLowerCase(Locale.ROOT);
        int index = -1;

        if (StringUtils.hasText(normalizedQuestion)) {
            index = lowerSource.indexOf(normalizedQuestion);
        }

        if (index < 0 && tokens != null) {
            for (String token : tokens) {
                if (!StringUtils.hasText(token)) {
                    continue;
                }
                int candidate = lowerSource.indexOf(token);
                if (candidate >= 0) {
                    index = candidate;
                    break;
                }
            }
        }

        if (index < 0) {
            return source.substring(0, maxLength) + "...";
        }

        int start = Math.max(0, index - maxLength / 3);
        int end = Math.min(source.length(), start + maxLength);

        if (end - start < maxLength && start > 0) {
            start = Math.max(0, end - maxLength);
        }

        String snippet = source.substring(start, end);

        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < source.length()) {
            snippet = snippet + "...";
        }

        return snippet;
    }

    private List<KnowledgeRetrievalHit> diversifyAndRank(List<KnowledgeRetrievalHit> hits, int limit) {
        if (hits == null || hits.isEmpty()) {
            return List.of();
        }

        List<KnowledgeRetrievalHit> sorted = hits.stream()
                .sorted(Comparator
                        .comparing(KnowledgeRetrievalHit::getScore).reversed()
                        .thenComparing(KnowledgeRetrievalHit::getDocumentId, Comparator.nullsLast(Long::compareTo))
                        .thenComparing(KnowledgeRetrievalHit::getChunkIndex, Comparator.nullsLast(Integer::compareTo))
                        .thenComparing(KnowledgeRetrievalHit::getChunkId, Comparator.nullsLast(Long::compareTo)))
                .toList();

        List<KnowledgeRetrievalHit> selected = new ArrayList<>();
        Set<Long> selectedChunkIds = new HashSet<>();
        Map<String, Integer> documentHitCount = new HashMap<>();

        fillSelected(sorted, selected, selectedChunkIds, documentHitCount, limit, 1);
        fillSelected(sorted, selected, selectedChunkIds, documentHitCount, limit, 2);
        fillSelected(sorted, selected, selectedChunkIds, documentHitCount, limit, Integer.MAX_VALUE);

        List<KnowledgeRetrievalHit> result = new ArrayList<>();
        int rank = 1;

        for (KnowledgeRetrievalHit hit : selected) {
            result.add(copyWithRank(hit, rank++));
        }

        return result;
    }

    private void fillSelected(
            List<KnowledgeRetrievalHit> sorted,
            List<KnowledgeRetrievalHit> selected,
            Set<Long> selectedChunkIds,
            Map<String, Integer> documentHitCount,
            int limit,
            int maxPerDocument
    ) {
        for (KnowledgeRetrievalHit hit : sorted) {
            if (selected.size() >= limit) {
                return;
            }

            if (hit.getChunkId() != null && selectedChunkIds.contains(hit.getChunkId())) {
                continue;
            }

            String key = buildDocumentKey(hit);
            int current = documentHitCount.getOrDefault(key, 0);
            if (current >= maxPerDocument) {
                continue;
            }

            selected.add(hit);

            if (hit.getChunkId() != null) {
                selectedChunkIds.add(hit.getChunkId());
            }

            documentHitCount.put(key, current + 1);
        }
    }

    private String buildDocumentKey(KnowledgeRetrievalHit hit) {
        if (hit.getDocumentId() != null) {
            return "D:" + hit.getDocumentId();
        }
        if (hit.getChunkId() != null) {
            return "C:" + hit.getChunkId();
        }
        return "U:" + System.identityHashCode(hit);
    }

    private KnowledgeRetrievalHit copyWithRank(KnowledgeRetrievalHit hit, int rankNo) {
        return KnowledgeRetrievalHit.builder()
                .knowledgeBaseId(hit.getKnowledgeBaseId())
                .knowledgeBaseName(hit.getKnowledgeBaseName())
                .documentId(hit.getDocumentId())
                .documentTitle(hit.getDocumentTitle())
                .chunkId(hit.getChunkId())
                .chunkIndex(hit.getChunkIndex())
                .chunkContent(hit.getChunkContent())
                .snippet(hit.getSnippet())
                .score(hit.getScore())
                .rankNo(rankNo)
                .knowledgeBase(hit.getKnowledgeBase())
                .document(hit.getDocument())
                .chunk(hit.getChunk())
                .build();
    }

    private String buildChunkTitle(KnowledgeRetrievalHit hit) {
        if (hit.getDocumentTitle() == null) {
            return hit.getChunkIndex() == null ? null : "块-" + hit.getChunkIndex();
        }
        if (hit.getChunkIndex() == null) {
            return hit.getDocumentTitle();
        }
        return hit.getDocumentTitle() + " #块" + hit.getChunkIndex();
    }

    @Getter
    public static class RetrievalResult {
        private final List<Long> knowledgeBaseIds;
        private final List<KnowledgeRetrievalHit> hits;

        public RetrievalResult(List<Long> knowledgeBaseIds, List<KnowledgeRetrievalHit> hits) {
            this.knowledgeBaseIds = knowledgeBaseIds == null ? List.of() : knowledgeBaseIds;
            this.hits = hits == null ? List.of() : hits;
        }

        public boolean hasHits() {
            return !hits.isEmpty();
        }
    }
}