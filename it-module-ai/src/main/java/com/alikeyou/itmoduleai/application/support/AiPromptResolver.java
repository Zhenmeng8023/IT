package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

@Component
@RequiredArgsConstructor
public class AiPromptResolver {

    private final AiPromptTemplateRepository aiPromptTemplateRepository;

    public AiPromptTemplate resolve(Long requestPromptTemplateId, String requestSceneCode, AiSession session) {
        if (requestPromptTemplateId != null) {
            return aiPromptTemplateRepository.findById(requestPromptTemplateId)
                    .orElseThrow(() -> new IllegalArgumentException("Prompt 模板不存在"));
        }
        if (session != null && session.getPromptTemplate() != null) {
            return session.getPromptTemplate();
        }

        String sceneCode = StringUtils.hasText(requestSceneCode)
                ? requestSceneCode.trim()
                : session != null ? session.getSceneCode() : null;

        if (!StringUtils.hasText(sceneCode)) {
            return null;
        }

        List<AiPromptTemplate> candidates = aiPromptTemplateRepository.findBySceneCodeAndIsEnabledTrueOrderByVersionNoDesc(sceneCode);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        return candidates.stream()
                .filter(item -> item.getPublishStatus() == AiPromptTemplate.PublishStatus.PUBLISHED)
                .findFirst()
                .orElse(candidates.get(0));
    }
}
