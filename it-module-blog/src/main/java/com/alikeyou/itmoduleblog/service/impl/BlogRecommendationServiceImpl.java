package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.dto.BlogRecommendationResult;
import com.alikeyou.itmoduleblog.dto.BlogResponse;
import com.alikeyou.itmoduleblog.entity.Blog;
import com.alikeyou.itmoduleblog.exception.BlogException;
import com.alikeyou.itmoduleblog.repository.BlogRepository;
import com.alikeyou.itmoduleblog.service.BlogRecommendationService;
import com.alikeyou.itmoduleblog.service.BlogService;
import com.alikeyou.itmodulerecommend.dto.BlogRecommendationSnapshot;
import com.alikeyou.itmodulerecommend.service.RecommendationResultService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BlogRecommendationServiceImpl implements BlogRecommendationService {

    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 12;
    private static final int MAX_CANDIDATE_POOL = 24;
    private static final String BLOG_STATUS_PUBLISHED = "published";

    @Autowired
    private BlogRepository blogRepository;
    @Autowired
    private BlogService blogService;
    @Autowired
    private RecommendationResultService recommendationResultService;

    @Override
    @Transactional(readOnly = true)
    public BlogRecommendationResult getRecommendations(Long blogId, Long viewerId, int size) {
        if (blogId == null) {
            throw new BlogException("博客 ID 不能为空");
        }

        int safeSize = Math.max(1, Math.min(size <= 0 ? DEFAULT_SIZE : size, MAX_SIZE));
        Blog currentBlog = blogRepository.findWithAssociationsById(blogId)
                .orElseThrow(() -> new BlogException("博客不存在，ID: " + blogId));

        BlogRecommendationSnapshot snapshot = recommendationResultService.getLatestBlogRecommendations(viewerId, safeSize);
        List<Blog> algorithmBlogs = loadAlgorithmBlogs(snapshot == null ? List.of() : snapshot.getBlogIds(), currentBlog.getId());
        List<Blog> contextBlogs = buildContextCandidateBlogs(
                currentBlog,
                algorithmBlogs.stream().map(Blog::getId).toList(),
                Math.min(MAX_CANDIDATE_POOL, Math.max(safeSize * 3, DEFAULT_SIZE * 2))
        );

        LinkedHashMap<Long, RecommendationCandidate> mergedCandidates = new LinkedHashMap<>();
        mergeCandidates(mergedCandidates, algorithmBlogs, true);
        mergeCandidates(mergedCandidates, contextBlogs, false);

        if (mergedCandidates.size() < safeSize) {
            List<Blog> supplementalBlogs = buildContextCandidateBlogs(currentBlog, mergedCandidates.keySet(), safeSize - mergedCandidates.size());
            mergeCandidates(mergedCandidates, supplementalBlogs, false);
        }

        List<Blog> rankedBlogs = rerankCandidates(currentBlog, mergedCandidates.values(), safeSize);

        BlogRecommendationResult result = new BlogRecommendationResult();
        result.setCurrentBlogId(blogId);
        result.setSource(resolveSource(algorithmBlogs, mergedCandidates));
        result.setAlgorithmVersion(snapshot == null ? null : snapshot.getAlgorithmVersion());
        result.setGeneratedAt(snapshot == null ? null : snapshot.getGeneratedAt());
        result.setTotal(rankedBlogs.size());
        result.setItems(rankedBlogs.stream()
                .map(blog -> blogService.convertToSecurePreviewResponse(blog, viewerId, false))
                .collect(Collectors.toList()));
        return result;
    }

    private List<Blog> loadAlgorithmBlogs(List<Long> orderedBlogIds, Long currentBlogId) {
        if (orderedBlogIds == null || orderedBlogIds.isEmpty()) {
            return List.of();
        }

        List<Long> filteredIds = orderedBlogIds.stream()
                .filter(Objects::nonNull)
                .filter(id -> !Objects.equals(id, currentBlogId))
                .distinct()
                .toList();

        if (filteredIds.isEmpty()) {
            return List.of();
        }

        Map<Long, Blog> blogMap = blogRepository.findByIdIn(filteredIds).stream()
                .filter(this::isPublished)
                .collect(Collectors.toMap(Blog::getId, blog -> blog, (left, right) -> left, LinkedHashMap::new));

        List<Blog> orderedBlogs = new ArrayList<>();
        for (Long blogId : filteredIds) {
            Blog blog = blogMap.get(blogId);
            if (blog != null) {
                orderedBlogs.add(blog);
            }
        }
        return orderedBlogs;
    }

    private List<Blog> buildContextCandidateBlogs(Blog currentBlog, Collection<Long> existingIds, int neededSize) {
        if (neededSize <= 0) {
            return List.of();
        }

        Set<Long> blockedIds = new LinkedHashSet<>();
        blockedIds.add(currentBlog.getId());
        if (existingIds != null) {
            blockedIds.addAll(existingIds);
        }

        List<Blog> pooledBlogs = new ArrayList<>();
        if (currentBlog.getAuthor() != null && currentBlog.getAuthor().getId() != null) {
            pooledBlogs.addAll(blogRepository.findByAuthorId(currentBlog.getAuthor().getId()));
        }
        pooledBlogs.addAll(blogRepository.findByHotness());
        pooledBlogs.addAll(blogRepository.findByTimeDesc());

        Map<Long, Blog> uniqueCandidates = new LinkedHashMap<>();
        for (Blog blog : pooledBlogs) {
            if (blog == null || blog.getId() == null || blockedIds.contains(blog.getId()) || !isPublished(blog)) {
                continue;
            }
            uniqueCandidates.putIfAbsent(blog.getId(), blog);
        }

        Comparator<Blog> comparator = Comparator
                .comparingInt((Blog blog) -> similarityScore(currentBlog, blog)).reversed()
                .thenComparing(Comparator.comparingInt(this::hotnessScore).reversed())
                .thenComparing(Comparator.comparing(this::resolveSortTime, Comparator.nullsLast(Comparator.reverseOrder())));

        return uniqueCandidates.values().stream()
                .sorted(comparator)
                .limit(neededSize)
                .collect(Collectors.toList());
    }

    private void mergeCandidates(Map<Long, RecommendationCandidate> sink, List<Blog> blogs, boolean algorithmCandidate) {
        if (blogs == null || blogs.isEmpty()) {
            return;
        }
        int rank = 0;
        for (Blog blog : blogs) {
            if (blog == null || blog.getId() == null) {
                continue;
            }
            RecommendationCandidate existing = sink.get(blog.getId());
            if (existing == null) {
                sink.put(blog.getId(), new RecommendationCandidate(blog, algorithmCandidate, algorithmCandidate ? rank : Integer.MAX_VALUE));
            } else if (algorithmCandidate && !existing.fromAlgorithm()) {
                sink.put(blog.getId(), new RecommendationCandidate(existing.blog(), true, rank));
            }
            rank++;
        }
    }

    private List<Blog> rerankCandidates(Blog currentBlog,
                                        Collection<RecommendationCandidate> candidates,
                                        int limit) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        Comparator<RecommendationCandidate> comparator = Comparator
                .comparingInt((RecommendationCandidate candidate) -> similarityScore(currentBlog, candidate.blog())).reversed()
                .thenComparing(Comparator.comparingInt(this::algorithmBoost).reversed())
                .thenComparing(Comparator.comparingInt((RecommendationCandidate candidate) -> hotnessScore(candidate.blog())).reversed())
                .thenComparing(Comparator.comparing(
                        (RecommendationCandidate candidate) -> resolveSortTime(candidate.blog()),
                        Comparator.nullsLast(Comparator.reverseOrder())
                ));

        return candidates.stream()
                .sorted(comparator)
                .limit(limit)
                .map(RecommendationCandidate::blog)
                .collect(Collectors.toList());
    }

    private int algorithmBoost(RecommendationCandidate candidate) {
        if (candidate == null || !candidate.fromAlgorithm()) {
            return 0;
        }
        return Math.max(0, MAX_CANDIDATE_POOL - candidate.algorithmRank());
    }

    private int similarityScore(Blog currentBlog, Blog candidate) {
        int score = 0;
        if (currentBlog == null || candidate == null) {
            return score;
        }

        if (currentBlog.getAuthor() != null
                && candidate.getAuthor() != null
                && Objects.equals(currentBlog.getAuthor().getId(), candidate.getAuthor().getId())) {
            score += 80;
        }

        Set<String> currentTags = normalizeTags(currentBlog.getTags());
        Set<String> candidateTags = normalizeTags(candidate.getTags());
        if (!currentTags.isEmpty() && !candidateTags.isEmpty()) {
            Set<String> intersection = new LinkedHashSet<>(currentTags);
            intersection.retainAll(candidateTags);
            score += intersection.size() * 25;
        }

        if (samePriceTier(currentBlog.getPrice(), candidate.getPrice())) {
            score += 5;
        }

        if (shareKeyword(buildKeywordText(currentBlog), buildKeywordText(candidate))) {
            score += 8;
        }
        return score;
    }

    private String buildKeywordText(Blog blog) {
        if (blog == null) {
            return null;
        }
        return String.join(" ",
                Objects.toString(blog.getTitle(), ""),
                Objects.toString(blog.getSummary(), ""));
    }

    private boolean shareKeyword(String currentTitle, String candidateTitle) {
        if (!StringUtils.hasText(currentTitle) || !StringUtils.hasText(candidateTitle)) {
            return false;
        }
        Set<String> currentWords = splitWords(currentTitle);
        if (currentWords.isEmpty()) {
            return false;
        }
        Set<String> candidateWords = splitWords(candidateTitle);
        currentWords.retainAll(candidateWords);
        return !currentWords.isEmpty();
    }

    private Set<String> splitWords(String text) {
        if (!StringUtils.hasText(text)) {
            return Set.of();
        }
        return java.util.Arrays.stream(text.trim().toLowerCase().split("[\\s,，、|/]+"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Set<String> normalizeTags(Map<String, String> tags) {
        if (tags == null || tags.isEmpty()) {
            return Set.of();
        }
        return tags.values().stream()
                .filter(StringUtils::hasText)
                .map(tag -> tag.trim().toLowerCase())
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private boolean samePriceTier(Integer currentPrice, Integer candidatePrice) {
        return resolvePriceTier(currentPrice) == resolvePriceTier(candidatePrice);
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

    private int hotnessScore(Blog blog) {
        int viewCount = blog.getViewCount() == null ? 0 : blog.getViewCount();
        int likeCount = blog.getLikeCount() == null ? 0 : blog.getLikeCount();
        int collectCount = blog.getCollectCount() == null ? 0 : blog.getCollectCount();
        int downloadCount = blog.getDownloadCount() == null ? 0 : blog.getDownloadCount();
        return viewCount + likeCount * 5 + collectCount * 10 + downloadCount * 8;
    }

    private Instant resolveSortTime(Blog blog) {
        if (blog == null) {
            return null;
        }
        return blog.getPublishTime() != null ? blog.getPublishTime() : blog.getCreatedAt();
    }

    private boolean isPublished(Blog blog) {
        if (blog == null || blog.getId() == null || !StringUtils.hasText(blog.getStatus())) {
            return false;
        }
        return BLOG_STATUS_PUBLISHED.equalsIgnoreCase(blog.getStatus().trim());
    }

    private String resolveSource(List<Blog> algorithmBlogs, Map<Long, RecommendationCandidate> mergedBlogs) {
        if (algorithmBlogs == null || algorithmBlogs.isEmpty()) {
            return "fallback";
        }
        if (mergedBlogs != null && mergedBlogs.size() > algorithmBlogs.size()) {
            return "mixed";
        }
        return "algorithm";
    }

    private record RecommendationCandidate(Blog blog, boolean fromAlgorithm, int algorithmRank) {}
}
