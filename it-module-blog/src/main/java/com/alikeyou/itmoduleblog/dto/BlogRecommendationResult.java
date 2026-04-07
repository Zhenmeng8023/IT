package com.alikeyou.itmoduleblog.dto;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class BlogRecommendationResult {

    private Long currentBlogId;

    private String source;

    private String algorithmVersion;

    private Instant generatedAt;

    private Integer total;

    private List<BlogResponse> items = new ArrayList<>();
}
