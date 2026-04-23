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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    public Page<KnowledgeBase> pageKnowledgeBasesByOwner(Long ownerId, Pageable pageable) {
        if (ownerId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "ownerId is required");
        }
        if (hasAdminKnowledgeAuthority()) {
            return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        return knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(currentUserId, pageable);
    }

    public Page<KnowledgeBase> pageKnowledgeBasesByProject(Long projectId, Pageable pageable) {
        if (projectId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "projectId is required");
        }
        if (hasAdminKnowledgeAuthority()) {
            return knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
        }
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        return knowledgeBaseRepository.findAccessibleProjectPage(projectId, currentUserId, pageable);
    }

    private KnowledgeBase requireKnowledgeBaseAccess(Long knowledgeBaseId, AccessLevel accessLevel) {
        KnowledgeBase knowledgeBase = loadKnowledgeBaseRequired(knowledgeBaseId);
        Long currentUserId = currentUserProvider.requireCurrentUserId();
        RoleLevel role = resolveRole(knowledgeBase);
        if (isAllowed(knowledgeBase, role, accessLevel, currentUserId)) {
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
        if (hasAdminKnowledgeAuthority()) {
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

    private boolean isAllowed(KnowledgeBase knowledgeBase,
                              RoleLevel role,
                              AccessLevel accessLevel,
                              Long currentUserId) {
        if (role == RoleLevel.ADMIN) {
            return true;
        }
        return switch (accessLevel) {
            case READ -> role == RoleLevel.OWNER
                    || role == RoleLevel.EDITOR
                    || role == RoleLevel.VIEWER
                    || isReadableByVisibility(knowledgeBase, currentUserId);
            case EDIT -> role == RoleLevel.OWNER || role == RoleLevel.EDITOR;
            case OWNER -> role == RoleLevel.OWNER;
        };
    }

    private boolean isReadableByVisibility(KnowledgeBase knowledgeBase, Long currentUserId) {
        if (knowledgeBase == null || currentUserId == null) {
            return false;
        }
        KnowledgeBase.Visibility visibility = knowledgeBase.getVisibility();
        if (visibility == null) {
            return false;
        }
        return switch (visibility) {
            case PUBLIC -> true;
            case TEAM -> knowledgeBase.getScopeType() == KnowledgeBase.ScopeType.PROJECT
                    && knowledgeBase.getProjectId() != null;
            case PRIVATE -> false;
        };
    }

    private boolean hasAdminKnowledgeAuthority() {
        return currentUserProvider.hasAuthority("view:admin:ai:knowledge");
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
