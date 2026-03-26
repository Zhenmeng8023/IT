package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiModel;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.repository.AiModelRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiModelSelector {

    private final AiModelRepository aiModelRepository;

    public AiModel select(Long requestModelId, AiSession session, AiPromptTemplate promptTemplate) {
        if (requestModelId != null) {
            return aiModelRepository.findById(requestModelId)
                    .orElseThrow(() -> new IllegalArgumentException("模型不存在"));
        }
        if (session != null && session.getActiveModel() != null) {
            return session.getActiveModel();
        }
        if (promptTemplate != null && promptTemplate.getDefaultModel() != null) {
            return promptTemplate.getDefaultModel();
        }
        return aiModelRepository.findFirstByIsEnabledTrueOrderByPriorityAscIdAsc()
                .orElseThrow(() -> new IllegalStateException("没有启用的 AI 模型"));
    }
}
