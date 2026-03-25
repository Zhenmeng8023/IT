package com.alikeyou.itmoduleai.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiCallLogDTO {
    private Long userId;
    private Long conversationId;
    private Long messageId;
    private Long promptTemplateId;
    private Long aiModelId;
    private String requestType;
    private String requestText;
    private String responseText;
    private Map<String, Object> requestParams;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal costAmount;
    private Integer latencyMs;
    private String status;
    private String errorCode;
    private String errorMessage;
}