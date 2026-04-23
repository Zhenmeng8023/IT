package com.alikeyou.itmoduleai.service.impl;

import com.alikeyou.itmoduleai.application.support.AiCurrentUserProvider;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageCapabilityStatusDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageGovernanceActionDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageQuotaConfigDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserQueryDTO;
import com.alikeyou.itmoduleai.repository.KnowledgeUsageAdminRepository;
import com.alikeyou.itmoduleai.service.admin.impl.KnowledgeUsageAdminServiceImpl;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeUsageAdminServiceImplTest {

    private UserInfoRepository userInfoRepository;
    private KnowledgeUsageAdminRepository knowledgeUsageAdminRepository;
    private AiCurrentUserProvider currentUserProvider;
    private KnowledgeUsageAdminServiceImpl service;

    @BeforeEach
    void setUp() {
        userInfoRepository = mock(UserInfoRepository.class);
        knowledgeUsageAdminRepository = mock(KnowledgeUsageAdminRepository.class);
        currentUserProvider = mock(AiCurrentUserProvider.class);
        service = new KnowledgeUsageAdminServiceImpl(userInfoRepository, knowledgeUsageAdminRepository, currentUserProvider);
    }

    @Test
    void pageUsersMergesAggregatedStatsAndPolicyFlags() {
        Pageable pageable = Pageable.ofSize(10);
        when(knowledgeUsageAdminRepository.pageUsers(any(), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(KnowledgeUsageAdminRepository.UserPolicyRow.builder()
                        .userId(7L)
                        .username("tom")
                        .nickname("Tom")
                        .email("tom@example.com")
                        .userStatus("ACTIVE")
                        .roleId(2)
                        .frozen(false)
                        .importEnabled(true)
                        .qaEnabled(false)
                        .maxKnowledgeBaseCount(9)
                        .maxDocumentCount(80)
                        .maxDailyQuestionCount(12)
                        .maxDailyImportCount(3)
                        .build()), pageable, 1));
        when(knowledgeUsageAdminRepository.queryKnowledgeAggregates(List.of(7L)))
                .thenReturn(java.util.Map.of(7L, KnowledgeUsageAdminRepository.KnowledgeAggregateRow.builder()
                        .userId(7L)
                        .knowledgeBaseCount(2)
                        .documentCount(12)
                        .chunkCount(40)
                        .lastKnowledgeUpdateAt(Instant.parse("2026-04-20T00:00:00Z"))
                        .build()));
        when(knowledgeUsageAdminRepository.queryMemberAggregates(List.of(7L)))
                .thenReturn(java.util.Map.of(7L, KnowledgeUsageAdminRepository.MemberAggregateRow.builder()
                        .userId(7L)
                        .memberKnowledgeBaseCount(5)
                        .build()));
        when(knowledgeUsageAdminRepository.queryCallAggregates(List.of(7L)))
                .thenReturn(java.util.Map.of(7L, KnowledgeUsageAdminRepository.CallAggregateRow.builder()
                        .userId(7L)
                        .qaCallCount(18L)
                        .qaCountLast7Days(7L)
                        .qaCountThisMonth(11L)
                        .totalTokens(560L)
                        .tokenCountThisMonth(240L)
                        .lastQaAt(Instant.parse("2026-04-21T00:00:00Z"))
                        .build()));
        when(knowledgeUsageAdminRepository.queryImportAggregates(List.of(7L)))
                .thenReturn(java.util.Map.of(7L, KnowledgeUsageAdminRepository.ImportAggregateRow.builder()
                        .userId(7L)
                        .importCount(9L)
                        .importCountLast7Days(3L)
                        .importCountThisMonth(6L)
                        .lastImportAt(Instant.parse("2026-04-19T00:00:00Z"))
                        .build()));

        Page<?> result = service.pageUsers(new KnowledgeUsageUserQueryDTO(), pageable);

        assertEquals(1, result.getTotalElements());
        Object first = result.getContent().get(0);
        assertEquals("tom", ((com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO) first).getUsername());
        assertEquals(2, ((com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO) first).getKnowledgeBaseCount());
        assertEquals(5, ((com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO) first).getMemberKnowledgeBaseCount());
        assertFalse(((com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO) first).getQaEnabled());
        assertEquals("RISK", ((com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO) first).getUsageStatus());
    }

    @Test
    void getUserStatusUsesDefaultPolicyAndExistingAggregates() {
        UserInfo user = new UserInfo();
        user.setId(11L);
        user.setUsername("alice");
        user.setNickname("Alice");
        user.setEmail("alice@example.com");
        user.setStatus("ACTIVE");
        user.setRoleId(1);
        when(userInfoRepository.findByIdWithAssociations(11L)).thenReturn(Optional.of(user));
        when(knowledgeUsageAdminRepository.getPolicyOrDefault(11L))
                .thenReturn(KnowledgeUsageAdminRepository.PolicyRow.builder()
                        .userId(11L)
                        .frozen(false)
                        .importEnabled(true)
                        .qaEnabled(true)
                        .maxKnowledgeBaseCount(20)
                        .maxDocumentCount(500)
                        .maxDailyQuestionCount(200)
                        .maxDailyImportCount(50)
                        .build());
        when(knowledgeUsageAdminRepository.queryKnowledgeAggregates(List.of(11L)))
                .thenReturn(java.util.Map.of(11L, KnowledgeUsageAdminRepository.KnowledgeAggregateRow.builder()
                        .userId(11L)
                        .knowledgeBaseCount(1)
                        .documentCount(3)
                        .chunkCount(10)
                        .build()));
        when(knowledgeUsageAdminRepository.queryMemberAggregates(List.of(11L)))
                .thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryCallAggregates(List.of(11L)))
                .thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryImportAggregates(List.of(11L)))
                .thenReturn(java.util.Map.of());

        KnowledgeUsageCapabilityStatusDTO result = service.getUserStatus(11L);

        assertTrue(result.getKnowledgeEnabled());
        assertEquals(1, result.getKnowledgeBaseCount());
        assertEquals(0, result.getMemberKnowledgeBaseCount());
        assertEquals(200, result.getMaxDailyQuestionCount());
        assertEquals("NORMAL", result.getUsageStatus());
    }

    @Test
    void updateQuotaDelegatesToRepository() {
        UserInfo user = new UserInfo();
        user.setId(15L);
        when(userInfoRepository.findByIdWithAssociations(15L)).thenReturn(Optional.of(user));
        when(currentUserProvider.resolveCurrentUserId()).thenReturn(99L);
        when(knowledgeUsageAdminRepository.getPolicyOrDefault(15L))
                .thenReturn(KnowledgeUsageAdminRepository.PolicyRow.builder()
                        .userId(15L)
                        .frozen(false)
                        .importEnabled(true)
                        .qaEnabled(true)
                        .maxKnowledgeBaseCount(8)
                        .maxDocumentCount(60)
                        .maxDailyQuestionCount(15)
                        .maxDailyImportCount(4)
                        .build());
        when(knowledgeUsageAdminRepository.queryKnowledgeAggregates(List.of(15L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryMemberAggregates(List.of(15L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryCallAggregates(List.of(15L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryImportAggregates(List.of(15L))).thenReturn(java.util.Map.of());

        KnowledgeUsageQuotaConfigDTO request = new KnowledgeUsageQuotaConfigDTO();
        request.setMaxKnowledgeBaseCount(8);
        request.setMaxDocumentCount(60);

        service.updateQuota(15L, request);

        verify(knowledgeUsageAdminRepository).updateQuota(eq(15L), eq(request), eq(99L));
    }

    @Test
    void disableQaDelegatesGovernanceAction() {
        UserInfo user = new UserInfo();
        user.setId(18L);
        when(userInfoRepository.findByIdWithAssociations(18L)).thenReturn(Optional.of(user));
        when(currentUserProvider.resolveCurrentUserId()).thenReturn(77L);
        when(knowledgeUsageAdminRepository.getPolicyOrDefault(18L))
                .thenReturn(KnowledgeUsageAdminRepository.PolicyRow.builder()
                        .userId(18L)
                        .frozen(false)
                        .importEnabled(true)
                        .qaEnabled(false)
                        .maxKnowledgeBaseCount(20)
                        .maxDocumentCount(500)
                        .maxDailyQuestionCount(200)
                        .maxDailyImportCount(50)
                        .build());
        when(knowledgeUsageAdminRepository.queryKnowledgeAggregates(List.of(18L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryMemberAggregates(List.of(18L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryCallAggregates(List.of(18L))).thenReturn(java.util.Map.of());
        when(knowledgeUsageAdminRepository.queryImportAggregates(List.of(18L))).thenReturn(java.util.Map.of());

        KnowledgeUsageGovernanceActionDTO action = new KnowledgeUsageGovernanceActionDTO();
        action.setReason("manual block");
        service.disableQa(18L, action);

        verify(knowledgeUsageAdminRepository).updateGovernance(18L, null, null, false, 77L, "DISABLE_QA", "manual block");
    }

    @Test
    void getUserStatusThrowsWhenUserMissing() {
        when(userInfoRepository.findByIdWithAssociations(88L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () -> service.getUserStatus(88L));

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }
}
