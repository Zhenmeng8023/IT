package com.alikeyou.itmoduleai.service.admin.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageCapabilityStatusDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageGovernanceActionDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageQuotaConfigDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserQueryDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO;
import com.alikeyou.itmoduleai.repository.KnowledgeUsageAdminRepository;
import com.alikeyou.itmoduleai.service.admin.KnowledgeUsageAdminService;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeUsageAdminServiceImpl implements KnowledgeUsageAdminService {

    private final UserInfoRepository userInfoRepository;
    private final KnowledgeUsageAdminRepository knowledgeUsageAdminRepository;
    private final AiCurrentUserProvider currentUserProvider;

    @Override
    @Transactional(readOnly = true)
    public Page<KnowledgeUsageUserStatsDTO> pageUsers(KnowledgeUsageUserQueryDTO query, Pageable pageable) {
        Page<KnowledgeUsageAdminRepository.UserPolicyRow> page = knowledgeUsageAdminRepository.pageUsers(query, pageable);
        List<Long> userIds = page.getContent().stream()
                .map(KnowledgeUsageAdminRepository.UserPolicyRow::getUserId)
                .toList();
        Map<Long, KnowledgeUsageAdminRepository.KnowledgeAggregateRow> knowledgeAggregates =
                knowledgeUsageAdminRepository.queryKnowledgeAggregates(userIds);
        Map<Long, KnowledgeUsageAdminRepository.MemberAggregateRow> memberAggregates =
                knowledgeUsageAdminRepository.queryMemberAggregates(userIds);
        Map<Long, KnowledgeUsageAdminRepository.CallAggregateRow> callAggregates =
                knowledgeUsageAdminRepository.queryCallAggregates(userIds);

        List<KnowledgeUsageUserStatsDTO> content = page.getContent().stream()
                .map(row -> toUserStats(row,
                        knowledgeAggregates.get(row.getUserId()),
                        memberAggregates.get(row.getUserId()),
                        callAggregates.get(row.getUserId())))
                .toList();
        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    @Override
    @Transactional(readOnly = true)
    public KnowledgeUsageCapabilityStatusDTO getUserStatus(Long userId) {
        UserInfo user = requireUser(userId);
        return buildStatus(user);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO updateQuota(Long userId, KnowledgeUsageQuotaConfigDTO quotaConfig) {
        requireUser(userId);
        knowledgeUsageAdminRepository.updateQuota(userId, quotaConfig == null ? new KnowledgeUsageQuotaConfigDTO() : quotaConfig, currentUserProvider.resolveCurrentUserId());
        return getUserStatus(userId);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO freezeUser(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, true, null, null, "FREEZE", action);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO unfreezeUser(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, false, null, null, "UNFREEZE", action);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO disableImport(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, null, false, null, "DISABLE_IMPORT", action);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO enableImport(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, null, true, null, "ENABLE_IMPORT", action);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO disableQa(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, null, null, false, "DISABLE_QA", action);
    }

    @Override
    public KnowledgeUsageCapabilityStatusDTO enableQa(Long userId, KnowledgeUsageGovernanceActionDTO action) {
        return updateGovernance(userId, null, null, true, "ENABLE_QA", action);
    }

    private KnowledgeUsageCapabilityStatusDTO updateGovernance(Long userId,
                                                               Boolean frozen,
                                                               Boolean importEnabled,
                                                               Boolean qaEnabled,
                                                               String actionCode,
                                                               KnowledgeUsageGovernanceActionDTO action) {
        requireUser(userId);
        knowledgeUsageAdminRepository.updateGovernance(
                userId,
                frozen,
                importEnabled,
                qaEnabled,
                currentUserProvider.resolveCurrentUserId(),
                actionCode,
                action == null ? null : action.getReason()
        );
        return getUserStatus(userId);
    }

    private UserInfo requireUser(Long userId) {
        return userInfoRepository.findByIdWithAssociations(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found"));
    }

    private KnowledgeUsageCapabilityStatusDTO buildStatus(UserInfo user) {
        Long userId = user.getId();
        KnowledgeUsageAdminRepository.PolicyRow policy = knowledgeUsageAdminRepository.getPolicyOrDefault(userId);
        Map<Long, KnowledgeUsageAdminRepository.KnowledgeAggregateRow> knowledgeAggregates =
                knowledgeUsageAdminRepository.queryKnowledgeAggregates(List.of(userId));
        Map<Long, KnowledgeUsageAdminRepository.MemberAggregateRow> memberAggregates =
                knowledgeUsageAdminRepository.queryMemberAggregates(List.of(userId));
        Map<Long, KnowledgeUsageAdminRepository.CallAggregateRow> callAggregates =
                knowledgeUsageAdminRepository.queryCallAggregates(List.of(userId));

        KnowledgeUsageAdminRepository.KnowledgeAggregateRow knowledge = knowledgeAggregates.get(userId);
        KnowledgeUsageAdminRepository.MemberAggregateRow member = memberAggregates.get(userId);
        KnowledgeUsageAdminRepository.CallAggregateRow calls = callAggregates.get(userId);

        return KnowledgeUsageCapabilityStatusDTO.builder()
                .userId(userId)
                .username(user.getUsername())
                .nickname(user.getNickname())
                .email(user.getEmail())
                .userStatus(user.getStatus())
                .roleId(user.getRoleId())
                .lastLoginAt(user.getLastLoginAt())
                .lastActiveAt(user.getLastActiveAt())
                .knowledgeEnabled(!Boolean.TRUE.equals(policy.getFrozen()))
                .frozen(Boolean.TRUE.equals(policy.getFrozen()))
                .importEnabled(Boolean.TRUE.equals(policy.getImportEnabled()))
                .qaEnabled(Boolean.TRUE.equals(policy.getQaEnabled()))
                .knowledgeBaseCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getKnowledgeBaseCount, 0))
                .memberKnowledgeBaseCount(valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0))
                .documentCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0))
                .chunkCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getChunkCount, 0))
                .qaCallCount(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCallCount, 0L))
                .totalTokens(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTotalTokens, 0L))
                .lastQaAt(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null))
                .lastKnowledgeUpdateAt(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getLastKnowledgeUpdateAt, null))
                .maxKnowledgeBaseCount(policy.getMaxKnowledgeBaseCount())
                .maxDocumentCount(policy.getMaxDocumentCount())
                .maxDailyQuestionCount(policy.getMaxDailyQuestionCount())
                .maxDailyImportCount(policy.getMaxDailyImportCount())
                .remark(policy.getRemark())
                .policyUpdatedAt(policy.getUpdatedAt())
                .policyUpdatedBy(policy.getUpdatedBy())
                .build();
    }

    private KnowledgeUsageUserStatsDTO toUserStats(KnowledgeUsageAdminRepository.UserPolicyRow row,
                                                   KnowledgeUsageAdminRepository.KnowledgeAggregateRow knowledge,
                                                   KnowledgeUsageAdminRepository.MemberAggregateRow member,
                                                   KnowledgeUsageAdminRepository.CallAggregateRow calls) {
        return KnowledgeUsageUserStatsDTO.builder()
                .userId(row.getUserId())
                .username(row.getUsername())
                .nickname(row.getNickname())
                .email(row.getEmail())
                .userStatus(row.getUserStatus())
                .roleId(row.getRoleId())
                .knowledgeEnabled(!Boolean.TRUE.equals(row.getFrozen()))
                .frozen(Boolean.TRUE.equals(row.getFrozen()))
                .importEnabled(Boolean.TRUE.equals(row.getImportEnabled()))
                .qaEnabled(Boolean.TRUE.equals(row.getQaEnabled()))
                .knowledgeBaseCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getKnowledgeBaseCount, 0))
                .memberKnowledgeBaseCount(valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0))
                .documentCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0))
                .chunkCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getChunkCount, 0))
                .qaCallCount(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCallCount, 0L))
                .totalTokens(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTotalTokens, 0L))
                .lastQaAt(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null))
                .lastKnowledgeUpdateAt(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getLastKnowledgeUpdateAt, null))
                .maxKnowledgeBaseCount(row.getMaxKnowledgeBaseCount())
                .maxDocumentCount(row.getMaxDocumentCount())
                .maxDailyQuestionCount(row.getMaxDailyQuestionCount())
                .maxDailyImportCount(row.getMaxDailyImportCount())
                .build();
    }

    private <T, R> R valueOrDefault(T source, Function<T, R> extractor, R defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        R value = extractor.apply(source);
        return value == null ? defaultValue : value;
    }
}
