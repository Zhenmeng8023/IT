package com.alikeyou.itmoduleinteractive.service;

import com.alikeyou.itmoduleinteractive.dto.UserActivityHeatmapResponseDTO;

public interface UserActivityService {
    long getUserReceivedLikes(Long userId);

    long getUserCollectCount(Long userId);

    long getUserHistoryCount(Long userId);

    UserActivityHeatmapResponseDTO getUserActivityHeatmap(Long userId, int days);
}
