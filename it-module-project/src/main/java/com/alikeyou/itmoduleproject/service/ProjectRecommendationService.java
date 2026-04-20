package com.alikeyou.itmoduleproject.service;

import com.alikeyou.itmoduleproject.vo.ProjectRecommendationResultVO;

public interface ProjectRecommendationService {

    ProjectRecommendationResultVO getRecommendations(Long projectId, Long currentUserId, int size, boolean forceRefresh);
}
