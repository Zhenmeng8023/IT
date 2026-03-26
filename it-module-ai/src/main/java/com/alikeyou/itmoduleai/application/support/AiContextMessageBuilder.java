package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.entity.AiMessage;
import com.alikeyou.itmoduleai.entity.AiPromptTemplate;
import com.alikeyou.itmoduleai.provider.model.AiProviderMessage;
import com.alikeyou.itmoduleai.repository.AiMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiContextMessageBuilder {

    private final AiMessageRepository aiMessageRepository;

    public List<AiProviderMessage> build(Long sessionId, AiPromptTemplate promptTemplate, String userContent) {
        List<AiProviderMessage> result = new ArrayList<>();
        if (promptTemplate != null && promptTemplate.getSystemPrompt() != null && !promptTemplate.getSystemPrompt().isBlank()) {
            result.add(AiProviderMessage.builder()
                    .role("system")
                    .content(promptTemplate.getSystemPrompt())
                    .build());
        }

        if (sessionId != null) {
            List<AiMessage> recent = aiMessageRepository.findBySession_IdOrderByCreatedAtDesc(sessionId, PageRequest.of(0, 20)).getContent();
            Collections.reverse(recent);
            for (AiMessage item : recent) {
                result.add(AiProviderMessage.builder()
                        .role(toRole(item.getRole()))
                        .content(item.getContent())
                        .build());
            }
        }

        result.add(AiProviderMessage.builder()
                .role("user")
                .content(userContent)
                .build());
        return result;
    }

    private String toRole(AiMessage.Role role) {
        if (role == null) {
            return "user";
        }
        return switch (role) {
            case SYSTEM -> "system";
            case USER -> "user";
            case ASSISTANT -> "assistant";
            case TOOL -> "tool";
        };
    }
}
