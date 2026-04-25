package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import com.alikeyou.itmoduleai.service.KnowledgeBaseService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseAdminController {

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeBaseService knowledgeBaseService;

    @GetMapping
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<Page<KnowledgeBase>> page(@RequestParam(required = false) Long ownerId,
                                                 @RequestParam(required = false) Long projectId,
                                                 @RequestParam(required = false) KnowledgeBase.ScopeType scopeType,
                                                 Pageable pageable) {
        Page<KnowledgeBase> page;
        if (scopeType != null && ownerId != null && projectId != null) {
            page = knowledgeBaseRepository.findByScopeTypeAndOwnerIdAndProjectIdOrderByUpdatedAtDesc(
                    scopeType, ownerId, projectId, pageable
            );
        } else if (scopeType != null && projectId != null) {
            page = knowledgeBaseRepository.findByScopeTypeAndProjectIdOrderByUpdatedAtDesc(
                    scopeType, projectId, pageable
            );
        } else if (scopeType != null && ownerId != null) {
            page = knowledgeBaseRepository.findByScopeTypeAndOwnerIdOrderByUpdatedAtDesc(
                    scopeType, ownerId, pageable
            );
        } else if (scopeType != null) {
            page = knowledgeBaseRepository.findByScopeTypeOrderByUpdatedAtDesc(scopeType, pageable);
        } else if (ownerId != null && projectId != null) {
            page = knowledgeBaseRepository.findByOwnerIdAndProjectIdOrderByUpdatedAtDesc(ownerId, projectId, pageable);
        } else if (projectId != null) {
            page = knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
        } else if (ownerId != null) {
            page = knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
        } else {
            page = knowledgeBaseRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }
        return ApiResponse.ok(page);
    }

    @PutMapping("/{id}/freeze")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeBase> freeze(@PathVariable Long id) {
        return ApiResponse.ok("知识库已冻结",
                knowledgeBaseService.updateKnowledgeBaseStatus(id, KnowledgeBase.Status.DISABLED));
    }

    @PutMapping("/{id}/archive")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeBase> archive(@PathVariable Long id) {
        return ApiResponse.ok("知识库已归档",
                knowledgeBaseService.updateKnowledgeBaseStatus(id, KnowledgeBase.Status.ARCHIVED));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        knowledgeBaseService.deleteKnowledgeBase(id);
        return ApiResponse.ok("知识库已删除", null);
    }
}
