package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.entity.KnowledgeBase;
import com.alikeyou.itmoduleai.repository.KnowledgeBaseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/knowledge-bases")
@RequiredArgsConstructor
public class KnowledgeBaseAdminController {

    private final KnowledgeBaseRepository knowledgeBaseRepository;

    @GetMapping
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<Page<KnowledgeBase>> page(@RequestParam(required = false) Long ownerId,
                                                 @RequestParam(required = false) Long projectId,
                                                 Pageable pageable) {
        Page<KnowledgeBase> page;
        if (projectId != null) {
            page = knowledgeBaseRepository.findByProjectIdOrderByUpdatedAtDesc(projectId, pageable);
        } else if (ownerId != null) {
            page = knowledgeBaseRepository.findByOwnerIdOrderByUpdatedAtDesc(ownerId, pageable);
        } else {
            page = knowledgeBaseRepository.findAllByOrderByUpdatedAtDesc(pageable);
        }
        return ApiResponse.ok(page);
    }
}
