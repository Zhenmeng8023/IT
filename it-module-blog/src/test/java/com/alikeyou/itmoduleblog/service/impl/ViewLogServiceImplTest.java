package com.alikeyou.itmoduleblog.service.impl;

import com.alikeyou.itmoduleblog.entity.ViewLog;
import com.alikeyou.itmoduleblog.repository.ViewLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ViewLogServiceImplTest {

    @Mock
    private ViewLogRepository viewLogRepository;

    private ViewLogServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ViewLogServiceImpl();
        ReflectionTestUtils.setField(service, "viewLogRepository", viewLogRepository);
    }

    @Test
    void recordBlogViewShouldPersistFirstAuthenticatedView() {
        when(viewLogRepository.findTopByUserIdAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(9L), eq("blog"), eq(101L), any(Instant.class)))
                .thenReturn(Optional.empty());

        boolean recorded = service.recordBlogView(101L, 9L, "127.0.0.1", "JUnit");

        assertTrue(recorded);
        ArgumentCaptor<ViewLog> captor = ArgumentCaptor.forClass(ViewLog.class);
        verify(viewLogRepository).save(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals(101L, captor.getValue().getTargetId());
    }

    @Test
    void recordBlogViewShouldSkipDuplicateAuthenticatedViewWithinWindow() {
        when(viewLogRepository.findTopByUserIdAndTargetTypeAndTargetIdAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq(9L), eq("blog"), eq(101L), any(Instant.class)))
                .thenReturn(Optional.of(new ViewLog()));

        boolean recorded = service.recordBlogView(101L, 9L, "127.0.0.1", "JUnit");

        assertFalse(recorded);
        verify(viewLogRepository, never()).save(any(ViewLog.class));
    }

    @Test
    void recordBlogViewShouldSkipDuplicateAnonymousViewWithinWindow() {
        when(viewLogRepository.findTopByUserIdIsNullAndTargetTypeAndTargetIdAndIpAddressAndUserAgentAndCreatedAtGreaterThanEqualOrderByCreatedAtDesc(
                eq("blog"), eq(101L), eq("127.0.0.1"), eq("JUnit"), any(Instant.class)))
                .thenReturn(Optional.of(new ViewLog()));

        boolean recorded = service.recordBlogView(101L, null, "127.0.0.1", "JUnit");

        assertFalse(recorded);
        verify(viewLogRepository, never()).save(any(ViewLog.class));
    }
}
