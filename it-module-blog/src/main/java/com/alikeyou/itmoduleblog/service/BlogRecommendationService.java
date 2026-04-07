package com.alikeyou.itmoduleblog.service;

import com.alikeyou.itmoduleblog.dto.BlogRecommendationResult;

public interface BlogRecommendationService {

    BlogRecommendationResult getRecommendations(Long blogId, Long viewerId, int size);
}
