package com.alikeyou.itmoduleproject.vo;

import lombok.Data;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectRecommendationResultVO {

    private Long currentProjectId;

    private String source;

    private String algorithmVersion;

    private Instant generatedAt;

    private Integer total;

    private List<ProjectListVO> items = new ArrayList<>();
}
