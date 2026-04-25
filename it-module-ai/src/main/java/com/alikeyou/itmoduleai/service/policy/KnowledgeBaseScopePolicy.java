package com.alikeyou.itmoduleai.service.policy;

import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

@Component
public class KnowledgeBaseScopePolicy {

    public KnowledgeBase.ScopeType normalizeScope(KnowledgeBase.ScopeType scopeType) {
        return scopeType == null ? KnowledgeBase.ScopeType.PERSONAL : scopeType;
    }

    public void validateScopeBinding(KnowledgeBase.ScopeType scopeType, Long ownerId, Long projectId) {
        KnowledgeBase.ScopeType effectiveScope = normalizeScope(scopeType);
        switch (effectiveScope) {
            case PERSONAL -> {
                if (ownerId == null) {
                    throw new IllegalArgumentException("ownerId is required when scopeType=PERSONAL");
                }
                if (projectId != null) {
                    throw new IllegalArgumentException("projectId must be null when scopeType=PERSONAL");
                }
            }
            case PROJECT -> {
                if (projectId == null) {
                    throw new IllegalArgumentException("projectId is required when scopeType=PROJECT");
                }
            }
            case PLATFORM -> {
                if (projectId != null) {
                    throw new IllegalArgumentException("projectId must be null when scopeType=PLATFORM");
                }
            }
        }
    }

    public KnowledgeBase requirePlatformScope(KnowledgeBase knowledgeBase) {
        if (knowledgeBase == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Knowledge base is required");
        }
        if (knowledgeBase.getScopeType() != KnowledgeBase.ScopeType.PLATFORM) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "This API only supports PLATFORM scope knowledge base");
        }
        return knowledgeBase;
    }
}
