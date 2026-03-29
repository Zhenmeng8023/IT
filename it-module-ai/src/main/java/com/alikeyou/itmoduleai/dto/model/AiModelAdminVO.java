package com.alikeyou.itmoduleai.dto.model;

import com.alikeyou.itmoduleai.entity.AiModel;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AiModelAdminVO {

    private Long id;
    private String modelName;
    private AiModel.ModelType modelType;
    private String providerCode;
    private AiModel.DeploymentMode deploymentMode;
    private String apiKeyMasked;
    private Boolean hasApiKey;
    private String baseUrl;
    private String defaultParams;
    private Integer priority;
    private Integer timeoutMs;
    private Boolean supportsStream;
    private Boolean supportsTools;
    private Boolean supportsEmbedding;
    private BigDecimal costInputPer1m;
    private BigDecimal costOutputPer1m;
    private Boolean isEnabled;
    private Boolean isActive;
    private String remark;
    private Instant createdAt;
    private Instant updatedAt;

    public static AiModelAdminVO from(AiModel entity, Long activeId) {
        if (entity == null) {
            return null;
        }
        return new AiModelAdminVO(
                entity.getId(),
                entity.getModelName(),
                entity.getModelType(),
                entity.getProviderCode(),
                entity.getDeploymentMode(),
                maskApiKey(entity.getApiKey()),
                StringUtils.hasText(entity.getApiKey()),
                entity.getBaseUrl(),
                entity.getDefaultParams(),
                entity.getPriority(),
                entity.getTimeoutMs(),
                entity.getSupportsStream(),
                entity.getSupportsTools(),
                entity.getSupportsEmbedding(),
                entity.getCostInputPer1m(),
                entity.getCostOutputPer1m(),
                entity.getIsEnabled(),
                activeId != null && activeId.equals(entity.getId()),
                entity.getRemark(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private static String maskApiKey(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return "";
        }
        String value = apiKey.trim();
        if (value.length() <= 8) {
            return "****";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
