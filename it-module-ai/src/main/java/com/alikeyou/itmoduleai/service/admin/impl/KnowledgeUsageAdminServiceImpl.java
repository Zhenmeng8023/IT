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

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

@Service
@RequiredArgsConstructor
@Transactional
public class KnowledgeUsageAdminServiceImpl implements KnowledgeUsageAdminService {

    private static final int DAYS_IN_MONTH = 30;
    private static final long DEFAULT_TOKEN_LIMIT_PER_QA = 2000L;

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
        Map<Long, KnowledgeUsageAdminRepository.ImportAggregateRow> importAggregates =
                knowledgeUsageAdminRepository.queryImportAggregates(userIds);

        List<KnowledgeUsageUserStatsDTO> content = page.getContent().stream()
                .map(row -> toUserStats(row,
                        knowledgeAggregates.get(row.getUserId()),
                        memberAggregates.get(row.getUserId()),
                        callAggregates.get(row.getUserId()),
                        importAggregates.get(row.getUserId())))
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
        Map<Long, KnowledgeUsageAdminRepository.ImportAggregateRow> importAggregates =
                knowledgeUsageAdminRepository.queryImportAggregates(List.of(userId));

        KnowledgeUsageAdminRepository.KnowledgeAggregateRow knowledge = knowledgeAggregates.get(userId);
        KnowledgeUsageAdminRepository.MemberAggregateRow member = memberAggregates.get(userId);
        KnowledgeUsageAdminRepository.CallAggregateRow calls = callAggregates.get(userId);
        KnowledgeUsageAdminRepository.ImportAggregateRow imports = importAggregates.get(userId);

        int personalKnowledgeBaseCount = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getPersonalKnowledgeBaseCount, 0);
        int projectKnowledgeBaseCount = valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0);
        int totalDocumentCount = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0)
                + valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getProjectDocumentCount, 0);
        int maxDocumentCountPerKnowledgeBase = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getMaxDocumentCountPerKnowledgeBase, 0);
        long qaCountThisMonth = valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCountThisMonth, 0L);
        long tokenCountThisMonth = valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTokenCountThisMonth, 0L);
        long importCountThisMonth = valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getImportCountThisMonth, 0L);
        int maxMonthlyQaCount = toMonthlyLimit(policy.getMaxDailyQuestionCount());
        int maxMonthlyImportCount = toMonthlyLimit(policy.getMaxDailyImportCount());
        long maxMonthlyTokenCount = computeMonthlyTokenLimit(maxMonthlyQaCount);
        UsageStatusResult usageStatus = resolveUsageStatus(
                user.getStatus(),
                Boolean.TRUE.equals(policy.getFrozen()),
                Boolean.TRUE.equals(policy.getImportEnabled()),
                Boolean.TRUE.equals(policy.getQaEnabled()),
                personalKnowledgeBaseCount,
                policy.getMaxKnowledgeBaseCount(),
                maxDocumentCountPerKnowledgeBase,
                policy.getMaxDocumentCount(),
                qaCountThisMonth,
                maxMonthlyQaCount,
                tokenCountThisMonth,
                maxMonthlyTokenCount,
                importCountThisMonth,
                maxMonthlyImportCount
        );
        Instant recentActiveAt = resolveRecentActiveAt(
                user.getLastActiveAt(),
                valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null),
                valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getLastImportAt, null),
                user.getLastLoginAt()
        );

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
                .personalKnowledgeBaseCount(personalKnowledgeBaseCount)
                .memberKnowledgeBaseCount(valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0))
                .projectKnowledgeBaseCount(projectKnowledgeBaseCount)
                .documentCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0))
                .totalDocumentCount(totalDocumentCount)
                .chunkCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getChunkCount, 0))
                .maxDocumentCountPerKnowledgeBase(maxDocumentCountPerKnowledgeBase)
                .qaCallCount(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCallCount, 0L))
                .qaCountLast7Days(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCountLast7Days, 0L))
                .qaCountThisMonth(qaCountThisMonth)
                .totalTokens(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTotalTokens, 0L))
                .tokenCountThisMonth(tokenCountThisMonth)
                .importCountLast7Days(valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getImportCountLast7Days, 0L))
                .importCountThisMonth(importCountThisMonth)
                .lastQaAt(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null))
                .lastImportAt(valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getLastImportAt, null))
                .recentActiveAt(recentActiveAt)
                .lastKnowledgeUpdateAt(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getLastKnowledgeUpdateAt, null))
                .maxKnowledgeBaseCount(policy.getMaxKnowledgeBaseCount())
                .maxDocumentCount(policy.getMaxDocumentCount())
                .maxDailyQuestionCount(policy.getMaxDailyQuestionCount())
                .maxDailyImportCount(policy.getMaxDailyImportCount())
                .maxMonthlyQaCount(maxMonthlyQaCount)
                .maxMonthlyImportCount(maxMonthlyImportCount)
                .maxMonthlyTokenCount(maxMonthlyTokenCount)
                .remark(policy.getRemark())
                .policyUpdatedAt(policy.getUpdatedAt())
                .policyUpdatedBy(policy.getUpdatedBy())
                .usageStatus(usageStatus.status())
                .riskReason(usageStatus.riskReason())
                .build();
    }

    private KnowledgeUsageUserStatsDTO toUserStats(KnowledgeUsageAdminRepository.UserPolicyRow row,
                                                   KnowledgeUsageAdminRepository.KnowledgeAggregateRow knowledge,
                                                   KnowledgeUsageAdminRepository.MemberAggregateRow member,
                                                   KnowledgeUsageAdminRepository.CallAggregateRow calls,
                                                   KnowledgeUsageAdminRepository.ImportAggregateRow imports) {
        int personalKnowledgeBaseCount = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getPersonalKnowledgeBaseCount, 0);
        int projectKnowledgeBaseCount = valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0);
        int totalDocumentCount = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0)
                + valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getProjectDocumentCount, 0);
        int maxDocumentCountPerKnowledgeBase = valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getMaxDocumentCountPerKnowledgeBase, 0);
        long qaCountThisMonth = valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCountThisMonth, 0L);
        long tokenCountThisMonth = valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTokenCountThisMonth, 0L);
        long importCountThisMonth = valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getImportCountThisMonth, 0L);
        int maxMonthlyQaCount = toMonthlyLimit(row.getMaxDailyQuestionCount());
        int maxMonthlyImportCount = toMonthlyLimit(row.getMaxDailyImportCount());
        long maxMonthlyTokenCount = computeMonthlyTokenLimit(maxMonthlyQaCount);
        UsageStatusResult usageStatus = resolveUsageStatus(
                row.getUserStatus(),
                Boolean.TRUE.equals(row.getFrozen()),
                Boolean.TRUE.equals(row.getImportEnabled()),
                Boolean.TRUE.equals(row.getQaEnabled()),
                personalKnowledgeBaseCount,
                row.getMaxKnowledgeBaseCount(),
                maxDocumentCountPerKnowledgeBase,
                row.getMaxDocumentCount(),
                qaCountThisMonth,
                maxMonthlyQaCount,
                tokenCountThisMonth,
                maxMonthlyTokenCount,
                importCountThisMonth,
                maxMonthlyImportCount
        );
        Instant recentActiveAt = resolveRecentActiveAt(
                row.getLastActiveAt(),
                valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null),
                valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getLastImportAt, null),
                row.getLastLoginAt()
        );

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
                .personalKnowledgeBaseCount(personalKnowledgeBaseCount)
                .memberKnowledgeBaseCount(valueOrDefault(member, KnowledgeUsageAdminRepository.MemberAggregateRow::getMemberKnowledgeBaseCount, 0))
                .projectKnowledgeBaseCount(projectKnowledgeBaseCount)
                .documentCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getDocumentCount, 0))
                .totalDocumentCount(totalDocumentCount)
                .chunkCount(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getChunkCount, 0))
                .maxDocumentCountPerKnowledgeBase(maxDocumentCountPerKnowledgeBase)
                .qaCallCount(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCallCount, 0L))
                .qaCountLast7Days(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getQaCountLast7Days, 0L))
                .qaCountThisMonth(qaCountThisMonth)
                .totalTokens(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getTotalTokens, 0L))
                .tokenCountThisMonth(tokenCountThisMonth)
                .importCountLast7Days(valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getImportCountLast7Days, 0L))
                .importCountThisMonth(importCountThisMonth)
                .lastQaAt(valueOrDefault(calls, KnowledgeUsageAdminRepository.CallAggregateRow::getLastQaAt, null))
                .lastImportAt(valueOrDefault(imports, KnowledgeUsageAdminRepository.ImportAggregateRow::getLastImportAt, null))
                .recentActiveAt(recentActiveAt)
                .lastKnowledgeUpdateAt(valueOrDefault(knowledge, KnowledgeUsageAdminRepository.KnowledgeAggregateRow::getLastKnowledgeUpdateAt, null))
                .maxKnowledgeBaseCount(row.getMaxKnowledgeBaseCount())
                .maxDocumentCount(row.getMaxDocumentCount())
                .maxDailyQuestionCount(row.getMaxDailyQuestionCount())
                .maxDailyImportCount(row.getMaxDailyImportCount())
                .maxMonthlyQaCount(maxMonthlyQaCount)
                .maxMonthlyImportCount(maxMonthlyImportCount)
                .maxMonthlyTokenCount(maxMonthlyTokenCount)
                .usageStatus(usageStatus.status())
                .riskReason(usageStatus.riskReason())
                .build();
    }

    private <T, R> R valueOrDefault(T source, Function<T, R> extractor, R defaultValue) {
        if (source == null) {
            return defaultValue;
        }
        R value = extractor.apply(source);
        return value == null ? defaultValue : value;
    }

    private int toMonthlyLimit(Integer dailyLimit) {
        if (dailyLimit == null || dailyLimit <= 0) {
            return 0;
        }
        return dailyLimit * DAYS_IN_MONTH;
    }

    private long computeMonthlyTokenLimit(int maxMonthlyQaCount) {
        if (maxMonthlyQaCount <= 0) {
            return 0L;
        }
        return maxMonthlyQaCount * DEFAULT_TOKEN_LIMIT_PER_QA;
    }

    private Instant resolveRecentActiveAt(Instant... values) {
        Instant latest = null;
        if (values == null) {
            return null;
        }
        for (Instant value : values) {
            if (value == null) {
                continue;
            }
            if (latest == null || value.isAfter(latest)) {
                latest = value;
            }
        }
        return latest;
    }

    private UsageStatusResult resolveUsageStatus(String userStatus,
                                                 boolean frozen,
                                                 boolean importEnabled,
                                                 boolean qaEnabled,
                                                 int personalKnowledgeBaseCount,
                                                 Integer maxKnowledgeBaseCount,
                                                 int maxDocumentCountPerKnowledgeBase,
                                                 Integer maxDocumentCount,
                                                 long qaCountThisMonth,
                                                 int maxMonthlyQaCount,
                                                 long tokenCountThisMonth,
                                                 long maxMonthlyTokenCount,
                                                 long importCountThisMonth,
                                                 int maxMonthlyImportCount) {
        List<String> riskReasons = new ArrayList<>();

        boolean overKnowledgeBaseLimit = isExceeded(personalKnowledgeBaseCount, maxKnowledgeBaseCount);
        if (overKnowledgeBaseLimit) {
            riskReasons.add("个人知识库数超出配额");
        }
        boolean overDocumentLimit = isExceeded(maxDocumentCountPerKnowledgeBase, maxDocumentCount);
        if (overDocumentLimit) {
            riskReasons.add("单知识库文档数超出配额");
        }
        boolean overQaLimit = isExceeded(qaCountThisMonth, maxMonthlyQaCount);
        if (overQaLimit) {
            riskReasons.add("月问答次数超出配额");
        }
        boolean overImportLimit = isExceeded(importCountThisMonth, maxMonthlyImportCount);
        if (overImportLimit) {
            riskReasons.add("月导入任务超出配额");
        }
        boolean overTokenLimit = isExceeded(tokenCountThisMonth, maxMonthlyTokenCount);
        if (overTokenLimit) {
            riskReasons.add("月 Token 使用量超出配额");
        }
        boolean overLimit = overKnowledgeBaseLimit || overDocumentLimit || overQaLimit || overImportLimit || overTokenLimit;

        boolean abnormalUserStatus = "DISABLED".equalsIgnoreCase(userStatus) || "BANNED".equalsIgnoreCase(userStatus);
        if (abnormalUserStatus) {
            riskReasons.add("账号状态异常");
        }
        if (!importEnabled) {
            riskReasons.add("导入能力被禁用");
        }
        if (!qaEnabled) {
            riskReasons.add("知识库问答能力被禁用");
        }

        boolean nearQaLimit = isNearLimit(qaCountThisMonth, maxMonthlyQaCount);
        if (nearQaLimit) {
            riskReasons.add("月问答次数接近配额上限");
        }
        boolean nearImportLimit = isNearLimit(importCountThisMonth, maxMonthlyImportCount);
        if (nearImportLimit) {
            riskReasons.add("月导入任务接近配额上限");
        }
        boolean nearTokenLimit = isNearLimit(tokenCountThisMonth, maxMonthlyTokenCount);
        if (nearTokenLimit) {
            riskReasons.add("月 Token 使用量接近配额上限");
        }

        String status;
        if (frozen) {
            status = "FROZEN";
        } else if (overLimit) {
            status = "OVER_LIMIT";
        } else if (abnormalUserStatus || !importEnabled || !qaEnabled || nearQaLimit || nearImportLimit || nearTokenLimit) {
            status = "RISK";
        } else {
            status = "NORMAL";
        }
        String riskReason = riskReasons.isEmpty() ? null : String.join("；", riskReasons);
        return new UsageStatusResult(status, riskReason);
    }

    private boolean isExceeded(long currentValue, Number limit) {
        if (limit == null) {
            return false;
        }
        long normalizedLimit = limit.longValue();
        if (normalizedLimit <= 0) {
            return false;
        }
        return currentValue > normalizedLimit;
    }

    private boolean isNearLimit(long currentValue, Number limit) {
        if (limit == null) {
            return false;
        }
        double normalizedLimit = limit.doubleValue();
        if (normalizedLimit <= 0) {
            return false;
        }
        return currentValue >= normalizedLimit * 0.9d;
    }

    private record UsageStatusResult(String status, String riskReason) {
    }
}
