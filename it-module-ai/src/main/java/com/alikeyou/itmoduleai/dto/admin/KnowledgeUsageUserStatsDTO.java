package com.alikeyou.itmoduleai.dto.admin;

import lombok.Builder;
import lombok.Getter;

import java.time.Instant;

@Getter
@Builder
public class KnowledgeUsageUserStatsDTO {

    private Long userId;

    private String username;

    private String nickname;

    private String email;

    private String userStatus;

    private Integer roleId;

    private Boolean knowledgeEnabled;

    private Boolean frozen;

    private Boolean importEnabled;

    private Boolean qaEnabled;

    private Integer knowledgeBaseCount;

    private Integer personalKnowledgeBaseCount;

    private Integer memberKnowledgeBaseCount;

    private Integer projectKnowledgeBaseCount;

    private Integer documentCount;

    private Integer totalDocumentCount;

    private Integer chunkCount;

    private Integer maxDocumentCountPerKnowledgeBase;

    private Long qaCallCount;

    private Long qaCountLast7Days;

    private Long qaCountThisMonth;

    private Long totalTokens;

    private Long tokenCountThisMonth;

    private Long importCountLast7Days;

    private Long importCountThisMonth;

    private Instant lastQaAt;

    private Instant lastImportAt;

    private Instant recentActiveAt;

    private Instant lastKnowledgeUpdateAt;

    private Integer maxKnowledgeBaseCount;

    private Integer maxDocumentCount;

    private Integer maxDailyQuestionCount;

    private Integer maxDailyImportCount;

    private Integer maxMonthlyQaCount;

    private Integer maxMonthlyImportCount;

    private Long maxMonthlyTokenCount;

    private String usageStatus;

    private String riskReason;
}
