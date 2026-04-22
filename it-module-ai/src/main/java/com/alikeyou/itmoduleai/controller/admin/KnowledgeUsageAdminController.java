package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageCapabilityStatusDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageGovernanceActionDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageQuotaConfigDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserQueryDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO;
import com.alikeyou.itmoduleai.dto.common.ApiResponse;
import com.alikeyou.itmoduleai.service.admin.KnowledgeUsageAdminService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai/knowledge-usage")
@RequiredArgsConstructor
public class KnowledgeUsageAdminController {

    private final KnowledgeUsageAdminService knowledgeUsageAdminService;

    @GetMapping("/users")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<Page<KnowledgeUsageUserStatsDTO>> pageUsers(@ModelAttribute KnowledgeUsageUserQueryDTO query, Pageable pageable) {
        return ApiResponse.ok(knowledgeUsageAdminService.pageUsers(query, pageable));
    }

    @GetMapping("/users/{userId}/status")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> getUserStatus(@PathVariable Long userId) {
        return ApiResponse.ok(knowledgeUsageAdminService.getUserStatus(userId));
    }

    @PutMapping("/users/{userId}/quota")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> updateQuota(@PathVariable Long userId,
                                                                      @RequestBody KnowledgeUsageQuotaConfigDTO request) {
        return ApiResponse.ok("配额更新成功", knowledgeUsageAdminService.updateQuota(userId, request));
    }

    @PostMapping("/users/{userId}/freeze")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> freeze(@PathVariable Long userId,
                                                                 @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库能力已冻结", knowledgeUsageAdminService.freezeUser(userId, request));
    }

    @PostMapping("/users/{userId}/unfreeze")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> unfreeze(@PathVariable Long userId,
                                                                   @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库能力已解冻", knowledgeUsageAdminService.unfreezeUser(userId, request));
    }

    @PostMapping("/users/{userId}/disable-import")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> disableImport(@PathVariable Long userId,
                                                                        @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库导入已禁用", knowledgeUsageAdminService.disableImport(userId, request));
    }

    @PostMapping("/users/{userId}/enable-import")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> enableImport(@PathVariable Long userId,
                                                                       @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库导入已恢复", knowledgeUsageAdminService.enableImport(userId, request));
    }

    @PostMapping("/users/{userId}/disable-qa")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> disableQa(@PathVariable Long userId,
                                                                    @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库问答已禁用", knowledgeUsageAdminService.disableQa(userId, request));
    }

    @PostMapping("/users/{userId}/enable-qa")
    @PreAuthorize("@aiPermissionGuard.canUseAdminKnowledgeDebug()")
    public ApiResponse<KnowledgeUsageCapabilityStatusDTO> enableQa(@PathVariable Long userId,
                                                                   @RequestBody(required = false) KnowledgeUsageGovernanceActionDTO request) {
        return ApiResponse.ok("用户知识库问答已恢复", knowledgeUsageAdminService.enableQa(userId, request));
    }
}
