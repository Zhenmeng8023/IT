package com.alikeyou.itmodulerecommend.service.impl;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulerecommend.dto.BlogRecommendationSnapshot;
import com.alikeyou.itmodulerecommend.entiey.RecommendationResult;
import com.alikeyou.itmodulerecommend.repository.RecommendationResultRepository;
import com.alikeyou.itmodulerecommend.service.RecommendationResultService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

@Service
public class RecommendationResultServiceImpl implements RecommendationResultService {

    private static final int MAX_LIMIT = 20;
    private static final int DEFAULT_GENERATION_LIMIT = 12;
    private static final Duration SNAPSHOT_TTL = Duration.ofHours(6);
    private static final String BLOG_TARGET_TYPE = "blog";
    private static final String BLOG_STATUS_PUBLISHED = "published";
    private static final String ALGORITHM_VERSION = "blog_hybrid_v1";
    private static final List<String> BLOG_COLLECTION_KEYS = List.of(
            "blog",
            "blogs",
            "blogList",
            "blogIds",
            "blog_ids",
            "recommendedBlogs",
            "recommended_blogs",
            "recommendedBlogIds",
            "recommended_blog_ids"
    );
    private static final List<String> GENERIC_COLLECTION_KEYS = List.of(
            "items",
            "recommendations",
            "results",
            "recommendedItems",
            "recommended_items"
    );
    private static final List<String> ID_FIELD_KEYS = List.of(
            "id",
            "blogId",
            "blog_id",
            "targetId",
            "target_id",
            "contentId",
            "content_id"
    );
    private static final Set<String> BLOG_TYPE_MARKERS = Set.of("blog", "blogs", "article", "articles");
    private static final TypeReference<Map<String, Object>> JSON_MAP_TYPE = new TypeReference<>() {};
    private static final String LOAD_PUBLISHED_BLOGS_SQL = """
            SELECT id, title, summary, tags, author_id, price, view_count, like_count, collect_count, download_count, publish_time, created_at
            FROM blog
            WHERE status = 'published'
            """;
    private static final String LOAD_USER_SIGNALS_SQL = """
            SELECT blog_id, signal_type, signal_time
            FROM (
                SELECT target_id AS blog_id, 'like' AS signal_type, created_at AS signal_time
                FROM like_record
                WHERE user_id = :userId AND target_type = 'blog'
                UNION ALL
                SELECT target_id AS blog_id, 'collect' AS signal_type, created_at AS signal_time
                FROM collect_record
                WHERE user_id = :userId AND target_type = 'blog'
                UNION ALL
                SELECT post_id AS blog_id, 'comment' AS signal_type, created_at AS signal_time
                FROM comment
                WHERE author_id = :userId AND post_type = 'blog' AND status <> 'deleted'
                UNION ALL
                SELECT target_id AS blog_id, 'view' AS signal_type, created_at AS signal_time
                FROM view_log
                WHERE user_id = :userId AND target_type = 'blog'
            ) signals
            ORDER BY signal_time DESC
            LIMIT 300
            """;

    @Autowired
    private RecommendationResultRepository recommendationResultRepository;
    @Autowired
    private NamedParameterJdbcTemplate namedParameterJdbcTemplate;
    @Autowired
    private EntityManager entityManager;
    @Autowired
    private ObjectMapper objectMapper;

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public BlogRecommendationSnapshot getLatestBlogRecommendations(Long userId, int limit) {
        int safeLimit = Math.max(1, Math.min(limit, MAX_LIMIT));
        if (userId == null) {
            return createEmptySnapshot(null);
        }

        RecommendationResult latest = recommendationResultRepository.findTopByUser_IdOrderByGeneratedAtDescIdDesc(userId)
                .orElse(null);
        BlogRecommendationSnapshot latestSnapshot = latest == null ? null : toSnapshot(latest, safeLimit);
        if (latest != null && isReusable(latest, latestSnapshot)) {
            return latestSnapshot;
        }

        RecommendationResult generated = generateAndPersist(userId, safeLimit);
        if (generated != null) {
            return toSnapshot(generated, safeLimit);
        }

        return latestSnapshot != null ? latestSnapshot : createEmptySnapshot(userId);
    }

    private boolean isReusable(RecommendationResult result, BlogRecommendationSnapshot snapshot) {
        if (result == null || snapshot == null || snapshot.getBlogIds() == null || snapshot.getBlogIds().isEmpty()) {
            return false;
        }
        if (result.getGeneratedAt() == null) {
            return false;
        }
        return result.getGeneratedAt().isAfter(Instant.now().minus(SNAPSHOT_TTL));
    }

    private RecommendationResult generateAndPersist(Long userId, int requestedLimit) {
        List<BlogVector> publishedBlogs = loadPublishedBlogs();
        if (publishedBlogs.isEmpty()) {
            return null;
        }

        int generationLimit = Math.min(MAX_LIMIT, Math.max(requestedLimit * 2, DEFAULT_GENERATION_LIMIT));
        List<BehaviorSignal> signals = loadBehaviorSignals(userId);
        List<ScoredRecommendation> scoredRecommendations = scoreBlogs(publishedBlogs, signals, generationLimit);
        if (scoredRecommendations.isEmpty()) {
            return null;
        }

        Instant now = Instant.now();
        RecommendationResult recommendationResult = new RecommendationResult();
        recommendationResult.setUser(entityManager.getReference(UserInfo.class, userId));
        recommendationResult.setAlgorithmVersion(ALGORITHM_VERSION);
        recommendationResult.setRecommendedItems(buildRecommendationItems(scoredRecommendations, signals, now));
        recommendationResult.setGeneratedAt(now);
        recommendationResult.setConsumed(Boolean.FALSE);
        recommendationResult.setConsumedAt(null);
        return recommendationResultRepository.save(recommendationResult);
    }

    private List<BlogVector> loadPublishedBlogs() {
        return namedParameterJdbcTemplate.query(LOAD_PUBLISHED_BLOGS_SQL, new MapSqlParameterSource(), (rs, rowNum) -> mapBlogVector(rs));
    }

    private BlogVector mapBlogVector(ResultSet rs) throws SQLException {
        BlogVector blog = new BlogVector();
        blog.id = rs.getLong("id");
        blog.title = rs.getString("title");
        blog.summary = rs.getString("summary");
        blog.authorId = rs.getObject("author_id") == null ? null : rs.getLong("author_id");
        blog.price = rs.getObject("price") == null ? 0 : rs.getInt("price");
        blog.viewCount = rs.getObject("view_count") == null ? 0 : rs.getInt("view_count");
        blog.likeCount = rs.getObject("like_count") == null ? 0 : rs.getInt("like_count");
        blog.collectCount = rs.getObject("collect_count") == null ? 0 : rs.getInt("collect_count");
        blog.downloadCount = rs.getObject("download_count") == null ? 0 : rs.getInt("download_count");
        blog.publishTime = toInstant(rs.getTimestamp("publish_time"));
        blog.createdAt = toInstant(rs.getTimestamp("created_at"));
        blog.tags = parseTags(rs.getString("tags"));
        return blog;
    }

    private List<BehaviorSignal> loadBehaviorSignals(Long userId) {
        MapSqlParameterSource parameters = new MapSqlParameterSource("userId", userId);
        return namedParameterJdbcTemplate.query(LOAD_USER_SIGNALS_SQL, parameters, (rs, rowNum) -> {
            BehaviorSignal signal = new BehaviorSignal();
            signal.blogId = rs.getLong("blog_id");
            signal.signalType = rs.getString("signal_type");
            signal.signalTime = toInstant(rs.getTimestamp("signal_time"));
            return signal;
        });
    }

    private List<ScoredRecommendation> scoreBlogs(List<BlogVector> publishedBlogs,
                                                  List<BehaviorSignal> signals,
                                                  int limit) {
        if (publishedBlogs == null || publishedBlogs.isEmpty()) {
            return List.of();
        }

        Map<Long, BlogVector> blogMap = publishedBlogs.stream()
                .collect(LinkedHashMap::new, (map, blog) -> map.put(blog.id, blog), LinkedHashMap::putAll);
        Map<Long, Double> authorWeights = new LinkedHashMap<>();
        Map<String, Double> tagWeights = new LinkedHashMap<>();
        Map<Integer, Double> priceTierWeights = new LinkedHashMap<>();
        Set<Long> interactedBlogIds = new LinkedHashSet<>();

        for (BehaviorSignal signal : signals) {
            BlogVector seedBlog = blogMap.get(signal.blogId);
            if (seedBlog == null) {
                continue;
            }
            double weight = computeSignalWeight(signal);
            interactedBlogIds.add(seedBlog.id);
            if (seedBlog.authorId != null) {
                authorWeights.merge(seedBlog.authorId, weight * 1.6d, Double::sum);
            }
            for (String tag : seedBlog.tags) {
                tagWeights.merge(tag, weight * 1.2d, Double::sum);
            }
            priceTierWeights.merge(resolvePriceTier(seedBlog.price), weight * 0.25d, Double::sum);
        }

        boolean hasPersonalSignal = !authorWeights.isEmpty() || !tagWeights.isEmpty() || !priceTierWeights.isEmpty();
        List<ScoredRecommendation> candidates = new ArrayList<>();

        for (BlogVector candidate : publishedBlogs) {
            if (candidate == null || candidate.id == null) {
                continue;
            }
            if (hasPersonalSignal && interactedBlogIds.contains(candidate.id)) {
                continue;
            }
            ScoredRecommendation scored = scoreCandidate(candidate, authorWeights, tagWeights, priceTierWeights, hasPersonalSignal);
            candidates.add(scored);
        }

        if (candidates.isEmpty()) {
            return List.of();
        }

        candidates.sort(Comparator
                .comparingDouble((ScoredRecommendation item) -> item.score).reversed()
                .thenComparingDouble(item -> item.hotnessScore).reversed()
                .thenComparing(item -> item.sortTime, Comparator.nullsLast(Comparator.reverseOrder())));

        List<ScoredRecommendation> top = candidates.stream()
                .limit(limit)
                .toList();

        if (top.isEmpty() && !publishedBlogs.isEmpty()) {
            return publishedBlogs.stream()
                    .map(blog -> scoreCandidate(blog, Map.of(), Map.of(), Map.of(), false))
                    .sorted(Comparator
                            .comparingDouble((ScoredRecommendation item) -> item.score).reversed()
                            .thenComparingDouble(item -> item.hotnessScore).reversed()
                            .thenComparing(item -> item.sortTime, Comparator.nullsLast(Comparator.reverseOrder())))
                    .limit(limit)
                    .toList();
        }
        return top;
    }

    private ScoredRecommendation scoreCandidate(BlogVector candidate,
                                                Map<Long, Double> authorWeights,
                                                Map<String, Double> tagWeights,
                                                Map<Integer, Double> priceTierWeights,
                                                boolean hasPersonalSignal) {
        double popularity = computePopularityScore(candidate);
        double freshness = computeFreshnessScore(candidate);
        double authorScore = candidate.authorId == null ? 0d : authorWeights.getOrDefault(candidate.authorId, 0d);
        double tagScore = 0d;
        List<String> matchedTags = new ArrayList<>();
        for (String tag : candidate.tags) {
            double contribution = tagWeights.getOrDefault(tag, 0d);
            if (contribution > 0d) {
                tagScore += contribution;
                matchedTags.add(tag);
            }
        }
        double priceScore = priceTierWeights.getOrDefault(resolvePriceTier(candidate.price), 0d);

        double totalScore = popularity * 1.4d + freshness;
        if (hasPersonalSignal) {
            totalScore += authorScore * 2.6d;
            totalScore += tagScore * 1.8d;
            totalScore += priceScore * 0.5d;
        }

        LinkedHashSet<String> reasons = new LinkedHashSet<>();
        if (authorScore > 0d) {
            reasons.add("偏好作者");
        }
        if (!matchedTags.isEmpty()) {
            reasons.add("命中标签:" + String.join("、", matchedTags.stream().limit(2).toList()));
        }
        if (popularity >= 2.5d) {
            reasons.add("热门内容");
        }
        if (freshness >= 1.5d) {
            reasons.add("近期发布");
        }
        if (reasons.isEmpty()) {
            reasons.add(hasPersonalSignal ? "综合兴趣匹配" : "热度兜底");
        }

        ScoredRecommendation scoredRecommendation = new ScoredRecommendation();
        scoredRecommendation.blogId = candidate.id;
        scoredRecommendation.score = round(totalScore);
        scoredRecommendation.hotnessScore = round(popularity);
        scoredRecommendation.sortTime = resolveSortTime(candidate);
        scoredRecommendation.reasons = new ArrayList<>(reasons);
        return scoredRecommendation;
    }

    private double computeSignalWeight(BehaviorSignal signal) {
        double baseWeight = switch (normalizeText(signal.signalType)) {
            case "collect" -> 4.5d;
            case "like" -> 3.0d;
            case "comment" -> 2.6d;
            default -> 1.0d;
        };
        if (signal.signalTime == null) {
            return baseWeight;
        }

        long days = Math.max(0L, ChronoUnit.DAYS.between(signal.signalTime, Instant.now()));
        if (days <= 3) {
            return baseWeight * 1.35d;
        }
        if (days <= 7) {
            return baseWeight * 1.2d;
        }
        if (days <= 30) {
            return baseWeight * 1.05d;
        }
        return baseWeight;
    }

    private double computePopularityScore(BlogVector blog) {
        int hotness = (blog.viewCount)
                + (blog.likeCount * 5)
                + (blog.collectCount * 10)
                + (blog.downloadCount * 8);
        return Math.log1p(Math.max(hotness, 0));
    }

    private double computeFreshnessScore(BlogVector blog) {
        Instant sortTime = resolveSortTime(blog);
        if (sortTime == null) {
            return 0d;
        }
        long days = Math.max(0L, ChronoUnit.DAYS.between(sortTime, Instant.now()));
        if (days <= 3) {
            return 2.2d;
        }
        if (days <= 7) {
            return 1.6d;
        }
        if (days <= 30) {
            return 1.0d;
        }
        return 0.2d;
    }

    private int resolvePriceTier(Integer price) {
        if (price == null || price == 0) {
            return 0;
        }
        if (price < 0) {
            return -1;
        }
        return 1;
    }

    private Instant resolveSortTime(BlogVector blog) {
        if (blog == null) {
            return null;
        }
        return blog.publishTime != null ? blog.publishTime : blog.createdAt;
    }

    private Map<String, Object> buildRecommendationItems(List<ScoredRecommendation> scoredRecommendations,
                                                         List<BehaviorSignal> signals,
                                                         Instant generatedAt) {
        List<Long> blogIds = scoredRecommendations.stream()
                .map(item -> item.blogId)
                .filter(Objects::nonNull)
                .toList();

        List<Map<String, Object>> blogs = scoredRecommendations.stream()
                .map(item -> {
                    Map<String, Object> payload = new LinkedHashMap<>();
                    payload.put("id", item.blogId);
                    payload.put("score", item.score);
                    payload.put("type", BLOG_TARGET_TYPE);
                    payload.put("reasons", item.reasons);
                    return payload;
                })
                .toList();

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("signalCount", signals == null ? 0 : signals.size());
        stats.put("hasPersonalSignal", signals != null && !signals.isEmpty());
        stats.put("generatedAt", generatedAt == null ? null : generatedAt.toString());

        Map<String, Object> recommendationItems = new LinkedHashMap<>();
        recommendationItems.put("scene", BLOG_TARGET_TYPE);
        recommendationItems.put("generator", ALGORITHM_VERSION);
        recommendationItems.put("blogIds", blogIds);
        recommendationItems.put("blogs", blogs);
        recommendationItems.put("stats", stats);
        return recommendationItems;
    }

    private BlogRecommendationSnapshot toSnapshot(RecommendationResult result, int limit) {
        BlogRecommendationSnapshot snapshot = createEmptySnapshot(result.getUser() != null ? result.getUser().getId() : null);
        snapshot.setAlgorithmVersion(result.getAlgorithmVersion());
        snapshot.setGeneratedAt(result.getGeneratedAt());

        List<Long> blogIds = extractBlogIds(result.getRecommendedItems(), limit);
        snapshot.setBlogIds(blogIds);
        snapshot.setFromAlgorithm(!blogIds.isEmpty());
        return snapshot;
    }

    private BlogRecommendationSnapshot createEmptySnapshot(Long userId) {
        BlogRecommendationSnapshot snapshot = new BlogRecommendationSnapshot();
        snapshot.setUserId(userId);
        snapshot.setFromAlgorithm(false);
        snapshot.setBlogIds(new ArrayList<>());
        return snapshot;
    }

    private List<Long> extractBlogIds(Map<String, Object> recommendedItems, int limit) {
        if (recommendedItems == null || recommendedItems.isEmpty()) {
            return List.of();
        }

        LinkedHashSet<Long> orderedIds = new LinkedHashSet<>();
        for (String key : BLOG_COLLECTION_KEYS) {
            addIdsFromValue(recommendedItems.get(key), orderedIds, true);
        }
        if (orderedIds.isEmpty()) {
            for (String key : GENERIC_COLLECTION_KEYS) {
                addIdsFromGenericValue(recommendedItems.get(key), orderedIds);
            }
        }

        if (orderedIds.isEmpty() && looksLikeTypedBlogMap(recommendedItems)) {
            addIdsFromValue(recommendedItems, orderedIds, true);
        }

        return orderedIds.stream().limit(limit).toList();
    }

    private void addIdsFromGenericValue(Object value, Set<Long> sink) {
        if (value == null || sink.size() >= MAX_LIMIT) {
            return;
        }
        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addIdsFromTypedValue(item, sink);
                if (sink.size() >= MAX_LIMIT) {
                    return;
                }
            }
            return;
        }
        if (value instanceof Object[] array) {
            for (Object item : array) {
                addIdsFromTypedValue(item, sink);
                if (sink.size() >= MAX_LIMIT) {
                    return;
                }
            }
            return;
        }
        addIdsFromTypedValue(value, sink);
    }

    private void addIdsFromTypedValue(Object value, Set<Long> sink) {
        if (value == null || sink.size() >= MAX_LIMIT) {
            return;
        }
        if (value instanceof Map<?, ?> map) {
            Long directId = extractIdFromMap(map, hasBlogMarker(map));
            if (directId != null) {
                sink.add(directId);
                return;
            }

            for (Object nestedValue : map.values()) {
                if (looksLikeTypedBlogMap(nestedValue)) {
                    addIdsFromValue(nestedValue, sink, true);
                }
            }
        }
    }

    private void addIdsFromValue(Object value, Set<Long> sink, boolean acceptUntypedMap) {
        if (value == null || sink.size() >= MAX_LIMIT) {
            return;
        }

        Long directId = parseId(value);
        if (directId != null) {
            sink.add(directId);
            return;
        }

        if (value instanceof Collection<?> collection) {
            for (Object item : collection) {
                addIdsFromValue(item, sink, acceptUntypedMap);
                if (sink.size() >= MAX_LIMIT) {
                    return;
                }
            }
            return;
        }

        if (value instanceof Object[] array) {
            for (Object item : array) {
                addIdsFromValue(item, sink, acceptUntypedMap);
                if (sink.size() >= MAX_LIMIT) {
                    return;
                }
            }
            return;
        }

        if (value instanceof Map<?, ?> map) {
            Long mapId = extractIdFromMap(map, acceptUntypedMap || hasBlogMarker(map));
            if (mapId != null) {
                sink.add(mapId);
                return;
            }

            if (looksLikeIdScoreMap(map)) {
                for (Object key : map.keySet()) {
                    Long keyId = parseId(key);
                    if (keyId != null) {
                        sink.add(keyId);
                    }
                    if (sink.size() >= MAX_LIMIT) {
                        return;
                    }
                }
                return;
            }

            for (Object nestedValue : map.values()) {
                addIdsFromValue(nestedValue, sink, acceptUntypedMap);
                if (sink.size() >= MAX_LIMIT) {
                    return;
                }
            }
        }
    }

    private Long extractIdFromMap(Map<?, ?> map, boolean allowUntyped) {
        if (map == null || map.isEmpty()) {
            return null;
        }

        if (!allowUntyped && !hasBlogMarker(map)) {
            return null;
        }

        for (String key : ID_FIELD_KEYS) {
            Long id = parseId(map.get(key));
            if (id != null) {
                return id;
            }
        }
        return null;
    }

    private boolean looksLikeIdScoreMap(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        int matchedIdKeys = 0;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            Long id = parseId(entry.getKey());
            if (id == null) {
                return false;
            }
            matchedIdKeys++;
            Object value = entry.getValue();
            if (value != null
                    && !(value instanceof Number)
                    && !(value instanceof Boolean)
                    && !(value instanceof CharSequence)) {
                return false;
            }
        }
        return matchedIdKeys > 0;
    }

    private boolean looksLikeTypedBlogMap(Object value) {
        return value instanceof Map<?, ?> map && hasBlogMarker(map);
    }

    private boolean hasBlogMarker(Map<?, ?> map) {
        if (map == null || map.isEmpty()) {
            return false;
        }
        return BLOG_TYPE_MARKERS.contains(normalizeText(map.get("type")))
                || BLOG_TYPE_MARKERS.contains(normalizeText(map.get("contentType")))
                || BLOG_TYPE_MARKERS.contains(normalizeText(map.get("targetType")))
                || BLOG_TYPE_MARKERS.contains(normalizeText(map.get("module")))
                || BLOG_TYPE_MARKERS.contains(normalizeText(map.get("scene")));
    }

    private Long parseId(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        if (rawValue instanceof Number number) {
            long value = number.longValue();
            return value > 0 ? value : null;
        }
        if (rawValue instanceof CharSequence text) {
            String normalized = text.toString().trim();
            if (!StringUtils.hasText(normalized)) {
                return null;
            }
            try {
                long parsed = Long.parseLong(normalized);
                return parsed > 0 ? parsed : null;
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }

    private String normalizeText(Object rawValue) {
        if (rawValue == null) {
            return null;
        }
        String value = Objects.toString(rawValue, "").trim().toLowerCase(Locale.ROOT);
        return StringUtils.hasText(value) ? value : null;
    }

    private Set<String> parseTags(String rawTags) {
        if (!StringUtils.hasText(rawTags)) {
            return Set.of();
        }
        try {
            Map<String, Object> map = objectMapper.readValue(rawTags, JSON_MAP_TYPE);
            LinkedHashSet<String> tags = new LinkedHashSet<>();
            for (Object value : map.values()) {
                String normalized = normalizeTag(value);
                if (normalized != null) {
                    tags.add(normalized);
                }
            }
            if (!tags.isEmpty()) {
                return tags;
            }
        } catch (Exception ignored) {
        }

        String cleaned = rawTags.trim()
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("'", "");
        LinkedHashSet<String> tags = new LinkedHashSet<>();
        for (String part : cleaned.split("[,，、|/]")) {
            String normalized = normalizeTag(part);
            if (normalized != null) {
                tags.add(normalized);
            }
        }
        return tags;
    }

    private String normalizeTag(Object rawTag) {
        if (rawTag == null) {
            return null;
        }
        String tag = Objects.toString(rawTag, "").trim().toLowerCase(Locale.ROOT);
        return StringUtils.hasText(tag) ? tag : null;
    }

    private Instant toInstant(Timestamp timestamp) {
        return timestamp == null ? null : timestamp.toInstant();
    }

    private double round(double value) {
        return Math.round(value * 100d) / 100d;
    }

    private static class BlogVector {
        private Long id;
        private String title;
        private String summary;
        private Long authorId;
        private Integer price;
        private int viewCount;
        private int likeCount;
        private int collectCount;
        private int downloadCount;
        private Instant publishTime;
        private Instant createdAt;
        private Set<String> tags = Set.of();
    }

    private static class BehaviorSignal {
        private Long blogId;
        private String signalType;
        private Instant signalTime;
    }

    private static class ScoredRecommendation {
        private Long blogId;
        private double score;
        private double hotnessScore;
        private Instant sortTime;
        private List<String> reasons = List.of();
    }
}
