package com.alikeyou.itmoduleproject.vo;

import lombok.Data;

import java.time.Instant;
import java.time.LocalDateTime;

@Data
public class AdminProjectRecommendationPageItemVO {

    private Long id;
    private String name;
    private String description;
    private String type;
    private String status;
    private String authorName;
    private String visibility;
    private String tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private TaskSnapshot task;

    @Data
    public static class TaskSnapshot {
        private String status;
        private String source;
        private String algorithmVersion;
        private Instant generatedAt;
        private Instant lastRunAt;
        private Integer resultTotal;
        private String errorMessage;
    }
}
