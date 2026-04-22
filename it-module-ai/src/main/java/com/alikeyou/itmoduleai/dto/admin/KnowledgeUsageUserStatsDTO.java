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

    private Integer memberKnowledgeBaseCount;

    private Integer documentCount;

    private Integer chunkCount;

    private Long qaCallCount;

    private Long totalTokens;

    private Instant lastQaAt;

    private Instant lastKnowledgeUpdateAt;

    private Integer maxKnowledgeBaseCount;

    private Integer maxDocumentCount;

    private Integer maxDailyQuestionCount;

    private Integer maxDailyImportCount;
}
