package com.alikeyou.itmodulerecommend.service;

import com.alikeyou.itmodulerecommend.dto.BlogRecommendationSnapshot;

public interface RecommendationResultService {

    BlogRecommendationSnapshot getLatestBlogRecommendations(Long userId, int limit);

    default BlogRecommendationSnapshot getLatestBlogRecommendations(Long userId, int limit, boolean forceRefresh) {
        return getLatestBlogRecommendations(userId, limit);
    }
}
