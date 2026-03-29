package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.dto.model.AiModelConnectionTestResult;
import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import com.alikeyou.itmoduleai.service.AiModelService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AiModelServiceImpl implements AiModelService {

    private final AiModelRepository aiModelRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AiModel> page(Pageable pageable) {
        return aiModelRepository.findAll(pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getById(Long id) {
        return aiModelRepository.findById(id).orElseThrow(() -> new RuntimeException("AI 模型不存在"));
    }

    @Override
    public AiModel save(AiModel entity) {
        Instant now = Instant.now();
        AiModel target;

        if (entity.getId() != null) {
            AiModel existed = getById(entity.getId());
            target = mergeForSave(existed, entity);
            target.setCreatedAt(existed.getCreatedAt());
        } else {
            target = mergeForSave(new AiModel(), entity);
            target.setCreatedAt(now);
        }

        target.setUpdatedAt(now);
        if (Boolean.TRUE.equals(target.getIsEnabled()) && target.getPriority() == null) {
            target.setPriority(nextEnabledPriority());
        }
        return aiModelRepository.save(target);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiModel> listEnabled() {
        return aiModelRepository.findByIsEnabledTrueOrderByPriorityAscIdAsc();
    }

    @Override
    @Transactional(readOnly = true)
    public AiModel getActiveModel() {
        return aiModelRepository.findFirstByIsEnabledTrueOrderByPriorityAscIdAsc()
                .orElseThrow(() -> new RuntimeException("没有启用的 AI 模型"));
    }

    @Override
    public AiModel enable(Long id) {
        AiModel current = getById(id);
        current.setIsEnabled(true);
        if (current.getPriority() == null) {
            current.setPriority(nextEnabledPriority());
        }
        current.setUpdatedAt(Instant.now());
        return aiModelRepository.save(current);
    }

    @Override
    public AiModel activate(Long id) {
        Instant now = Instant.now();
        AiModel target = getById(id);
        target.setIsEnabled(true);
        target.setUpdatedAt(now);

        List<AiModel> enabled = new ArrayList<>(aiModelRepository.findByIsEnabledTrueOrderByPriorityAscIdAsc());
        enabled.removeIf(item -> item.getId().equals(target.getId()));
        enabled.sort(Comparator
                .comparing((AiModel item) -> item.getPriority() == null ? Integer.MAX_VALUE : item.getPriority())
                .thenComparing(AiModel::getId));

        target.setPriority(0);
        aiModelRepository.save(target);

        int priority = 1;
        for (AiModel item : enabled) {
            item.setPriority(priority++);
            item.setUpdatedAt(now);
        }
        aiModelRepository.saveAll(enabled);
        return target;
    }

    @Override
    public void disable(Long id) {
        AiModel entity = getById(id);
        entity.setIsEnabled(false);
        entity.setUpdatedAt(Instant.now());
        aiModelRepository.save(entity);
    }

    @Override
    public AiModelConnectionTestResult testConnection(Long id) {
        AiModel model = getById(id);
        Instant start = Instant.now();
        String targetUrl = resolveTestUrl(model);

        if (!StringUtils.hasText(targetUrl)) {
            return new AiModelConnectionTestResult(false, "未配置可测试的 Base URL", null, null, 0L, "请先填写 Base URL", Instant.now());
        }

        try {
            HttpClient client = HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(10))
                    .build();

            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .timeout(Duration.ofMillis(resolveTimeoutMs(model)))
                    .GET()
                    .header("Accept", "application/json,text/plain,*/*");

            if (StringUtils.hasText(model.getApiKey()) && model.getDeploymentMode() == AiModel.DeploymentMode.REMOTE_API) {
                builder.header("Authorization", "Bearer " + model.getApiKey().trim());
            }

            HttpResponse<String> response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            int status = response.statusCode();
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            boolean reachable = status >= 200 && status < 500;
            String detail = clip(response.body());
            String message = reachable
                    ? (status >= 200 && status < 400 ? "连接成功" : "服务可达，已收到响应")
                    : "服务返回异常状态";

            return new AiModelConnectionTestResult(reachable, message, targetUrl, status, durationMs, detail, Instant.now());
        } catch (Exception e) {
            long durationMs = Duration.between(start, Instant.now()).toMillis();
            return new AiModelConnectionTestResult(false, "连接失败", targetUrl, null, durationMs, clip(e.getMessage()), Instant.now());
        }
    }

    private AiModel mergeForSave(AiModel target, AiModel source) {
        target.setModelName(normalize(source.getModelName()));
        target.setModelType(source.getModelType());
        target.setProviderCode(normalize(source.getProviderCode()));
        target.setDeploymentMode(source.getDeploymentMode());

        if (StringUtils.hasText(source.getApiKey())) {
            target.setApiKey(source.getApiKey().trim());
        } else if (target.getId() == null) {
            target.setApiKey(null);
        }

        target.setBaseUrl(normalize(source.getBaseUrl()));
        target.setDefaultParams(normalize(source.getDefaultParams()));
        target.setPriority(source.getPriority());
        target.setTimeoutMs(source.getTimeoutMs());
        target.setSupportsStream(source.getSupportsStream());
        target.setSupportsTools(source.getSupportsTools());
        target.setSupportsEmbedding(source.getSupportsEmbedding());
        target.setCostInputPer1m(source.getCostInputPer1m());
        target.setCostOutputPer1m(source.getCostOutputPer1m());
        target.setIsEnabled(source.getIsEnabled() != null ? source.getIsEnabled() : Boolean.FALSE);
        target.setRemark(normalize(source.getRemark()));
        return target;
    }

    private Integer nextEnabledPriority() {
        return aiModelRepository.findByIsEnabledTrueOrderByPriorityAscIdAsc().stream()
                .map(AiModel::getPriority)
                .filter(item -> item != null)
                .max(Integer::compareTo)
                .map(item -> item + 1)
                .orElse(0);
    }

    private long resolveTimeoutMs(AiModel model) {
        Integer timeoutMs = model.getTimeoutMs();
        if (timeoutMs == null || timeoutMs < 1000) {
            return 10000L;
        }
        return Math.min(timeoutMs.longValue(), 60000L);
    }

    private String resolveTestUrl(AiModel model) {
        String baseUrl = normalize(model.getBaseUrl());
        if (!StringUtils.hasText(baseUrl)) {
            return null;
        }
        if (model.getDeploymentMode() == AiModel.DeploymentMode.LOCAL_OLLAMA) {
            String clean = baseUrl.replaceAll("/+$", "");
            int apiIndex = clean.indexOf("/api/");
            if (apiIndex >= 0) {
                return clean.substring(0, apiIndex) + "/api/tags";
            }
            return clean + "/api/tags";
        }
        return baseUrl;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private String clip(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        String text = value.trim().replaceAll("\\s+", " ");
        return text.length() > 240 ? text.substring(0, 240) + "..." : text;
    }
}
