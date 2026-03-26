package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.entity.AiSession;
import com.alikeyou.itmoduleai.repository.AiPromptTemplateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AiPromptResolver {

    private final AiPromptTemplateRepository aiPromptTemplateRepository;

    public AiPromptTemplate resolve(Long requestPromptTemplateId, AiSession session) {
        if (requestPromptTemplateId != null) {
            return aiPromptTemplateRepository.findById(requestPromptTemplateId)
                    .orElseThrow(() -> new IllegalArgumentException("Prompt 模板不存在"));
        }
        if (session != null && session.getPromptTemplate() != null) {
            return session.getPromptTemplate();
        }
        return null;
    }
}
