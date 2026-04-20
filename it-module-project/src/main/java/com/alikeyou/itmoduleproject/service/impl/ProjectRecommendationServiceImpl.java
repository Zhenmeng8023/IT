package com.alikeyou.itmoduleproject.service.impl;

import com.alikeyou.itmoduleproject.service.ProjectRecommendationService;
import com.alikeyou.itmoduleproject.service.ProjectService;
import com.alikeyou.itmoduleproject.vo.ProjectListVO;
import com.alikeyou.itmoduleproject.vo.ProjectRecommendationResultVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
public class ProjectRecommendationServiceImpl implements ProjectRecommendationService {

    private static final int DEFAULT_SIZE = 6;
    private static final int MAX_SIZE = 20;
    private static final String SOURCE_ALGORITHM = "algorithm";
    private static final String SOURCE_FALLBACK = "fallback";
    private static final String ALGORITHM_VERSION = "project_related_v1";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    private final ProjectService projectService;
    private final Map<String, CachedRecommendation> cacheStore = new ConcurrentHashMap<>();

    @Override
    public ProjectRecommendationResultVO getRecommendations(Long projectId, Long currentUserId, int size, boolean forceRefresh) {
        int safeSize = normalizeSize(size);
        String cacheKey = buildCacheKey(projectId, currentUserId, safeSize);
        Instant now = Instant.now();

        if (!forceRefresh) {
            CachedRecommendation cached = cacheStore.get(cacheKey);
            if (cached != null && !isExpired(cached.generatedAt(), now)) {
                return copyResult(cached.result());
            }
        }

        List<ProjectListVO> relatedProjects = projectService.listRelatedProjects(projectId, currentUserId, safeSize);
        ProjectRecommendationResultVO result = new ProjectRecommendationResultVO();
        result.setCurrentProjectId(projectId);
        result.setSource(relatedProjects.isEmpty() ? SOURCE_FALLBACK : SOURCE_ALGORITHM);
        result.setAlgorithmVersion(ALGORITHM_VERSION);
        result.setGeneratedAt(now);
        result.setTotal(relatedProjects.size());
        result.setItems(new ArrayList<>(relatedProjects));

        cacheStore.put(cacheKey, new CachedRecommendation(now, copyResult(result)));
        return result;
    }

    private int normalizeSize(int size) {
        if (size <= 0) {
            return DEFAULT_SIZE;
        }
        return Math.max(1, Math.min(size, MAX_SIZE));
    }

    private String buildCacheKey(Long projectId, Long userId, int size) {
        return projectId + ":" + (userId == null ? "anonymous" : userId) + ":" + size;
    }

    private boolean isExpired(Instant generatedAt, Instant now) {
        return generatedAt == null || generatedAt.isBefore(now.minus(CACHE_TTL));
    }

    private ProjectRecommendationResultVO copyResult(ProjectRecommendationResultVO source) {
        ProjectRecommendationResultVO copy = new ProjectRecommendationResultVO();
        copy.setCurrentProjectId(source.getCurrentProjectId());
        copy.setSource(source.getSource());
        copy.setAlgorithmVersion(source.getAlgorithmVersion());
        copy.setGeneratedAt(source.getGeneratedAt());
        copy.setTotal(source.getTotal());
        copy.setItems(source.getItems() == null ? new ArrayList<>() : new ArrayList<>(source.getItems()));
        return copy;
    }

    private record CachedRecommendation(Instant generatedAt, ProjectRecommendationResultVO result) {}
}
