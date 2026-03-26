package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.application.support.model.KnowledgeRetrievalHit;
import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.entity.AiCallLog;
import com.alikeyou.itmoduleai.entity.AiRetrievalLog;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class AiKnowledgeResolver {

    private static final Pattern TOKEN_SPLIT_PATTERN = Pattern.compile("[^\\p{IsAlphabetic}\\p{IsDigit}\\u4e00-\\u9fa5]+");

    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;
    private final KnowledgeChunkRepository knowledgeChunkRepository;
    private final AiRetrievalLogRepository aiRetrievalLogRepository;

    public List<Long> resolveKnowledgeBaseIds(AiSession session, List<Long> requestKnowledgeBaseIds) {
        if (requestKnowledgeBaseIds != null && !requestKnowledgeBaseIds.isEmpty()) {
            return requestKnowledgeBaseIds.stream().distinct().toList();
        }

        Set<Long> ids = new LinkedHashSet<>();
        if (session != null && session.getDefaultKnowledgeBase() != null && session.getDefaultKnowledgeBase().getId() != null) {
            ids.add(session.getDefaultKnowledgeBase().getId());
        }
        if (session == null || session.getId() == null) {
            return new ArrayList<>(ids);
        }

        List<AiSessionKnowledgeBase> bindings = aiSessionKnowledgeBaseRepository.findBySession_IdOrderByPriorityAscIdAsc(session.getId());
        for (AiSessionKnowledgeBase item : bindings) {
            if (item.getKnowledgeBase() != null && item.getKnowledgeBase().getId() != null) {
                ids.add(item.getKnowledgeBase().getId());
            }
        }
        return new ArrayList<>(ids);
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(AiSession session,
                                    String userQuestion,
                                    List<Long> requestKnowledgeBaseIds,
                                    Integer topK) {
        List<Long> knowledgeBaseIds = resolveKnowledgeBaseIds(session, requestKnowledgeBaseIds);
        if (!StringUtils.hasText(userQuestion) || knowledgeBaseIds.isEmpty()) {
            return new RetrievalResult(knowledgeBaseIds, List.of());
        }

        List<String> tokens = tokenize(userQuestion);
        if (tokens.isEmpty()) {
            return new RetrievalResult(knowledgeBaseIds, List.of());
        }

        int candidateSize = Math.max(topK == null ? 0 : topK, 5) * 20;
        candidateSize = Math.min(Math.max(candidateSize, 50), 300);

        List<KnowledgeChunk> candidates = knowledgeChunkRepository.findRecentCandidatesByKnowledgeBaseIds(
                knowledgeBaseIds,
                PageRequest.of(0, candidateSize)
        );

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
                    .snippet(buildSnippet(chunk.getContent(), userQuestion, 220))
                    .score(score)
                    .knowledgeBase(chunk.getKnowledgeBase())
                    .document(chunk.getDocument())
                    .chunk(chunk)
                    .build());
        }

        List<KnowledgeRetrievalHit> ranked = hits.stream()
                .sorted(Comparator.comparing(KnowledgeRetrievalHit::getScore).reversed()
                        .thenComparing(KnowledgeRetrievalHit::getChunkId, Comparator.nullsLast(Long::compareTo)))
                .limit(Math.max(topK == null ? 0 : topK, 5))
                .toList();

        List<KnowledgeRetrievalHit> finalHits = new ArrayList<>();
        int rank = 1;
        for (KnowledgeRetrievalHit item : ranked) {
            finalHits.add(KnowledgeRetrievalHit.builder()
                    .knowledgeBaseId(item.getKnowledgeBaseId())
                    .knowledgeBaseName(item.getKnowledgeBaseName())
                    .documentId(item.getDocumentId())
                    .documentTitle(item.getDocumentTitle())
                    .chunkId(item.getChunkId())
                    .chunkIndex(item.getChunkIndex())
                    .chunkContent(item.getChunkContent())
                    .snippet(item.getSnippet())
                    .score(item.getScore())
                    .rankNo(rank++)
                    .knowledgeBase(item.getKnowledgeBase())
                    .document(item.getDocument())
                    .chunk(item.getChunk())
                    .build());
        }
        return new RetrievalResult(knowledgeBaseIds, finalHits);
    }

    public String buildKnowledgeAugmentedQuestion(String userQuestion, List<KnowledgeRetrievalHit> hits) {
        if (!StringUtils.hasText(userQuestion) || hits == null || hits.isEmpty()) {
            return userQuestion;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("请优先基于下面提供的知识片段回答用户问题。\n");
        sb.append("如果知识片段不足以支持结论，请明确说明\"根据当前知识库无法完全确认\"，不要捏造。\n\n");
        sb.append("【知识片段】\n");
        for (KnowledgeRetrievalHit hit : hits) {
            sb.append("[").append(hit.getRankNo()).append("] ")
                    .append(hit.getDocumentTitle() == null ? "未命名文档" : hit.getDocumentTitle());
            if (hit.getChunkIndex() != null) {
                sb.append(" #块").append(hit.getChunkIndex());
            }
            sb.append("\n");
            sb.append(hit.getChunkContent()).append("\n\n");
        }
        sb.append("【用户问题】\n").append(userQuestion).append("\n\n");
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
            log.setCreatedAt(Instant.now());
            logs.add(log);
        }
        aiRetrievalLogRepository.saveAll(logs);
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
        String kbName = normalize(chunk.getKnowledgeBase() == null ? null : chunk.getKnowledgeBase().getName());

        double score = 0D;
        if (StringUtils.hasText(normalizedQuestion) && StringUtils.hasText(content) && content.contains(normalizedQuestion)) {
            score += 8D;
        }
        for (String token : tokens) {
            if (!StringUtils.hasText(token)) {
                continue;
            }
            int contentCount = countContains(content, token);
            int titleCount = countContains(title, token);
            int kbCount = countContains(kbName, token);

            score += Math.min(contentCount, 6) * 1.2D;
            score += Math.min(titleCount, 3) * 2.3D;
            score += Math.min(kbCount, 2) * 1.5D;
        }

        if (StringUtils.hasText(content) && content.length() <= 600) {
            score += 0.3D;
        }
        if (chunk.getChunkIndex() != null && chunk.getChunkIndex() == 0) {
            score += 0.2D;
        }
        return BigDecimal.valueOf(score).setScale(6, RoundingMode.HALF_UP);
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

    private String buildSnippet(String content, String question, int maxLength) {
        if (!StringUtils.hasText(content)) {
            return null;
        }
        String normalizedQuestion = normalize(question);
        String source = content.trim();
        if (!StringUtils.hasText(normalizedQuestion) || source.length() <= maxLength) {
            return source.length() <= maxLength ? source : source.substring(0, maxLength) + "...";
        }

        String lowerSource = source.toLowerCase(Locale.ROOT);
        int idx = lowerSource.indexOf(normalizedQuestion);
        if (idx < 0) {
            return source.length() <= maxLength ? source : source.substring(0, maxLength) + "...";
        }
        int start = Math.max(0, idx - maxLength / 3);
        int end = Math.min(source.length(), start + maxLength);
        String snippet = source.substring(start, end);
        if (start > 0) {
            snippet = "..." + snippet;
        }
        if (end < source.length()) {
            snippet = snippet + "...";
        }
        return snippet;
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
