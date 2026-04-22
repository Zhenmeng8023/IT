package com.alikeyou.itmoduleai.service.admin;

import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageCapabilityStatusDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageGovernanceActionDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageQuotaConfigDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserQueryDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface KnowledgeUsageAdminService {

    Page<KnowledgeUsageUserStatsDTO> pageUsers(KnowledgeUsageUserQueryDTO query, Pageable pageable);

    KnowledgeUsageCapabilityStatusDTO getUserStatus(Long userId);

    KnowledgeUsageCapabilityStatusDTO updateQuota(Long userId, KnowledgeUsageQuotaConfigDTO quotaConfig);

    KnowledgeUsageCapabilityStatusDTO freezeUser(Long userId, KnowledgeUsageGovernanceActionDTO action);

    KnowledgeUsageCapabilityStatusDTO unfreezeUser(Long userId, KnowledgeUsageGovernanceActionDTO action);

    KnowledgeUsageCapabilityStatusDTO disableImport(Long userId, KnowledgeUsageGovernanceActionDTO action);

    KnowledgeUsageCapabilityStatusDTO enableImport(Long userId, KnowledgeUsageGovernanceActionDTO action);

    KnowledgeUsageCapabilityStatusDTO disableQa(Long userId, KnowledgeUsageGovernanceActionDTO action);

    KnowledgeUsageCapabilityStatusDTO enableQa(Long userId, KnowledgeUsageGovernanceActionDTO action);
}
