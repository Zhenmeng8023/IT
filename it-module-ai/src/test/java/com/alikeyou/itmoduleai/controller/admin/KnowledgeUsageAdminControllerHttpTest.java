package com.alikeyou.itmoduleai.controller.admin;

import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageCapabilityStatusDTO;
import com.alikeyou.itmoduleai.dto.admin.KnowledgeUsageUserStatsDTO;
import com.alikeyou.itmoduleai.service.admin.KnowledgeUsageAdminService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class KnowledgeUsageAdminControllerHttpTest {

    @Mock
    private KnowledgeUsageAdminService knowledgeUsageAdminService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new KnowledgeUsageAdminController(knowledgeUsageAdminService))
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .build();
    }

    @Test
    void pageUsersReturnsPagedStats() throws Exception {
        when(knowledgeUsageAdminService.pageUsers(any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(
                        List.of(KnowledgeUsageUserStatsDTO.builder()
                                .userId(3L)
                                .username("bob")
                                .knowledgeBaseCount(2)
                                .qaEnabled(true)
                                .build()),
                        PageRequest.of(0, 10),
                        1
                ));

        mockMvc.perform(get("/api/admin/ai/knowledge-usage/users")
                        .param("keyword", "bob")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[0].userId").value(3))
                .andExpect(jsonPath("$.data.content[0].username").value("bob"))
                .andExpect(jsonPath("$.data.content[0].knowledgeBaseCount").value(2));
    }

    @Test
    void getUserStatusReturnsCapabilitySnapshot() throws Exception {
        when(knowledgeUsageAdminService.getUserStatus(5L))
                .thenReturn(KnowledgeUsageCapabilityStatusDTO.builder()
                        .userId(5L)
                        .username("amy")
                        .frozen(false)
                        .importEnabled(true)
                        .qaEnabled(false)
                        .build());

        mockMvc.perform(get("/api/admin/ai/knowledge-usage/users/5/status"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(5))
                .andExpect(jsonPath("$.data.username").value("amy"))
                .andExpect(jsonPath("$.data.qaEnabled").value(false));
    }

    @Test
    void updateQuotaDelegatesToService() throws Exception {
        when(knowledgeUsageAdminService.updateQuota(eq(9L), any()))
                .thenReturn(KnowledgeUsageCapabilityStatusDTO.builder()
                        .userId(9L)
                        .maxKnowledgeBaseCount(6)
                        .build());

        mockMvc.perform(put("/api/admin/ai/knowledge-usage/users/9/quota")
                        .contentType("application/json")
                        .content("""
                                {
                                  "maxKnowledgeBaseCount": 6,
                                  "maxDocumentCount": 100
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(9))
                .andExpect(jsonPath("$.data.maxKnowledgeBaseCount").value(6));
    }

    @Test
    void disableQaDelegatesToService() throws Exception {
        when(knowledgeUsageAdminService.disableQa(eq(7L), any()))
                .thenReturn(KnowledgeUsageCapabilityStatusDTO.builder()
                        .userId(7L)
                        .qaEnabled(false)
                        .build());

        mockMvc.perform(post("/api/admin/ai/knowledge-usage/users/7/disable-qa")
                        .contentType("application/json")
                        .content("{\"reason\":\"risk\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.userId").value(7))
                .andExpect(jsonPath("$.data.qaEnabled").value(false));

        verify(knowledgeUsageAdminService).disableQa(eq(7L), any());
    }
}
