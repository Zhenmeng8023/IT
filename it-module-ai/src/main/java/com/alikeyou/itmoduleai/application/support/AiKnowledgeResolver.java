package com.alikeyou.itmoduleai.application.support;

import com.alikeyou.itmoduleai.dto.response.AiCitationResponse;
import com.alikeyou.itmoduleai.entity.AiSessionKnowledgeBase;
import com.alikeyou.itmoduleai.repository.AiSessionKnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AiKnowledgeResolver {

    private final AiSessionKnowledgeBaseRepository aiSessionKnowledgeBaseRepository;

    public List<Long> resolveKnowledgeBaseIds(Long sessionId, List<Long> requestKnowledgeBaseIds) {
        if (requestKnowledgeBaseIds != null && !requestKnowledgeBaseIds.isEmpty()) {
            return requestKnowledgeBaseIds;
        }
        if (sessionId == null) {
            return List.of();
        }
        List<AiSessionKnowledgeBase> bindings = aiSessionKnowledgeBaseRepository.findBySession_IdOrderByPriorityAscIdAsc(sessionId);
        List<Long> ids = new ArrayList<>();
        for (AiSessionKnowledgeBase item : bindings) {
            ids.add(item.getKnowledgeBase().getId());
        }
        return ids;
    }

    public List<AiCitationResponse> buildPlaceholderCitations(List<Long> knowledgeBaseIds) {
        List<AiCitationResponse> list = new ArrayList<>();
        if (knowledgeBaseIds == null) {
            return list;
        }
        for (Long knowledgeBaseId : knowledgeBaseIds) {
            list.add(AiCitationResponse.builder()
                    .knowledgeBaseId(knowledgeBaseId)
                    .build());
        }
        return list;
    }
}
