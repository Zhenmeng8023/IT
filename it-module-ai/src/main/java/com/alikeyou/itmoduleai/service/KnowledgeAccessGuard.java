package com.alikeyou.itmoduleai.service;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.entity.KnowledgeBaseMember;
import com.alikeyou.itmoduleai.entity.KnowledgeDocument;
import com.alikeyou.itmoduleai.entity.KnowledgeImportTask;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseMemberRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeDocumentRepository;
import com.alikeyou.itmoduleai.repository.KnowledgeImportTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Objects;

@Component
@RequiredArgsConstructor
public class KnowledgeAccessGuard {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseMemberRepository knowledgeBaseMemberRepository;
    private final KnowledgeDocumentRepository knowledgeDocumentRepository;
    private final KnowledgeImportTaskRepository knowledgeImportTaskRepository;
    private final AiCurrentUserProvider currentUserProvider;

    public KnowledgeBase requireKnowledgeBaseRead(Long knowledgeBaseId) {
        return requireKnowledgeBaseAccess(knowledgeBaseId, AccessLevel.READ);
    }

    public KnowledgeBase requireKnowledgeBaseEdit(Long knowledgeBaseId) {
        return requireKnowledgeBaseAccess(knowledgeBaseId, AccessLevel.EDIT);
    }

    public KnowledgeBase requireKnowledgeBaseOwner(Long knowledgeBaseId) {
        return requireKnowledgeBaseAccess(knowledgeBaseId, AccessLevel.OWNER);
    }

    public KnowledgeDocument requireDocumentRead(Long documentId) {
        KnowledgeDocument document = loadDocumentRequired(documentId);
        requireKnowledgeBaseRead(document.getKnowledgeBase().getId());
        return document;
    }

    public KnowledgeDocument requireDocumentEdit(Long documentId) {
        KnowledgeDocument document = loadDocumentRequired(documentId);
        requireKnowledgeBaseEdit(document.getKnowledgeBase().getId());
        return document;
    }

    public KnowledgeImportTask requireImportTaskRead(Long taskId) {
        KnowledgeImportTask task = loadImportTaskRequired(taskId);
        requireKnowledgeBaseRead(task.getKnowledgeBase().getId());
        return task;
    }

    public KnowledgeImportTask requireImportTaskEdit(Long taskId) {
        KnowledgeImportTask task = loadImportTaskRequired(taskId);
        requireKnowledgeBaseEdit(task.getKnowledgeBase().getId());
        return task;
    }

    public boolean hasKnowledgeBaseOwnerAccess(Long knowledgeBaseId) {
        KnowledgeBase knowledgeBase = loadKnowledgeBaseRequired(knowledgeBaseId);
        RoleLevel role = resolveRole(knowledgeBase);
        return role == RoleLevel.ADMIN || role == RoleLevel.OWNER;
    }

    private KnowledgeBase requireKnowledgeBaseAccess(Long knowledgeBaseId, AccessLevel accessLevel) {
        KnowledgeBase knowledgeBase = loadKnowledgeBaseRequired(knowledgeBaseId);
        RoleLevel role = resolveRole(knowledgeBase);
        if (isAllowed(role, accessLevel)) {
            return knowledgeBase;
        }
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "You do not have permission to access this knowledge base");
    }

    private KnowledgeBase loadKnowledgeBaseRequired(Long knowledgeBaseId) {
        if (knowledgeBaseId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "knowledgeBaseId is required");
        }
        return knowledgeBaseRepository.findById(knowledgeBaseId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge base not found"));
    }

    private KnowledgeDocument loadDocumentRequired(Long documentId) {
        if (documentId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "documentId is required");
        }
        return knowledgeDocumentRepository.findById(documentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge document not found"));
    }

    private KnowledgeImportTask loadImportTaskRequired(Long taskId) {
        if (taskId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "taskId is required");
        }
        return knowledgeImportTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Knowledge import task not found"));
    }

    private RoleLevel resolveRole(KnowledgeBase knowledgeBase) {
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        if (currentUserProvider.isAdminAiViewer()) {
            return RoleLevel.ADMIN;
        }
        if (Objects.equals(currentUserId, knowledgeBase.getOwnerId())) {
            return RoleLevel.OWNER;
        }
        return knowledgeBaseMemberRepository.findByKnowledgeBase_IdAndUserId(knowledgeBase.getId(), currentUserId)
                .map(KnowledgeBaseMember::getRoleCode)
                .map(this::mapRole)
                .orElse(RoleLevel.NONE);
    }

    private RoleLevel mapRole(KnowledgeBaseMember.RoleCode roleCode) {
        if (roleCode == null) {
            return RoleLevel.NONE;
        }
        return switch (roleCode) {
            case OWNER -> RoleLevel.OWNER;
            case EDITOR -> RoleLevel.EDITOR;
            case VIEWER -> RoleLevel.VIEWER;
        };
    }

    private boolean isAllowed(RoleLevel role, AccessLevel accessLevel) {
        if (role == RoleLevel.ADMIN) {
            return true;
        }
        return switch (accessLevel) {
            case READ -> role == RoleLevel.OWNER || role == RoleLevel.EDITOR || role == RoleLevel.VIEWER;
            case EDIT -> role == RoleLevel.OWNER || role == RoleLevel.EDITOR;
            case OWNER -> role == RoleLevel.OWNER;
        };
    }

    private enum AccessLevel {
        READ,
        EDIT,
        OWNER
    }

    private enum RoleLevel {
        ADMIN,
        OWNER,
        EDITOR,
        VIEWER,
        NONE
    }
}
