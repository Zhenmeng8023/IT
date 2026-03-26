package com.alikeyou.itmoduleai.provider;

import com.alikeyou.itmoduleai.entity.AiModel;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiProviderManager {

    private final List<AiProvider> providers;

    public AiProvider resolve(AiModel model) {
        return providers.stream()
                .filter(provider -> provider.supports(model))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("未找到可用的 AI Provider: " + (model == null ? null : model.getProviderCode())));
    }
}
