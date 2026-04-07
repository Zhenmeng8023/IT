package com.alikeyou.itmodulerecommend.dto;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlogRecommendationSnapshot {

    private Long userId;

    private String algorithmVersion;

    private Instant generatedAt;

    private boolean fromAlgorithm;

    private List<Long> blogIds = new ArrayList<>();
}
