package com.alikeyou.itmoduleai.repository;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class KnowledgeUsageAdminRepositoryTest {

    @Test
    void getPolicyOrDefaultReturnsDefaultsWithoutCreatingPolicyRow() {
        NamedParameterJdbcTemplate jdbcTemplate = mock(NamedParameterJdbcTemplate.class);
        KnowledgeUsageAdminRepository repository = new KnowledgeUsageAdminRepository(jdbcTemplate);
        when(jdbcTemplate.queryForObject(
                anyString(),
                any(MapSqlParameterSource.class),
                ArgumentMatchers.<RowMapper<KnowledgeUsageAdminRepository.PolicyRow>>any()))
                .thenThrow(new EmptyResultDataAccessException(1));

        KnowledgeUsageAdminRepository.PolicyRow policy = repository.getPolicyOrDefault(9L);

        assertEquals(9L, policy.getUserId());
        assertFalse(policy.getFrozen());
        assertTrue(policy.getImportEnabled());
        assertTrue(policy.getQaEnabled());
        assertEquals(20, policy.getMaxKnowledgeBaseCount());
        assertEquals(500, policy.getMaxDocumentCount());
        assertEquals(200, policy.getMaxDailyQuestionCount());
        assertEquals(50, policy.getMaxDailyImportCount());
        verify(jdbcTemplate, never()).update(anyString(), any(MapSqlParameterSource.class));
    }
}
