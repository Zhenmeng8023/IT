package com.alikeyou.itmoduleai.dto.admin;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class KnowledgeUsageQuotaConfigDTO {

    private Integer maxKnowledgeBaseCount;

    private Integer maxDocumentCount;

    private Integer maxDailyQuestionCount;

    private Integer maxDailyImportCount;

    private Integer maxMonthlyQaCount;

    private Integer maxMonthlyImportCount;

    private Long maxMonthlyTokenCount;

    private String remark;
}
