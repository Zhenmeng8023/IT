package com.alikeyou.itmoduleblog.service;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BlogAutoAuditServiceTest {

    private final BlogAutoAuditService service = new BlogAutoAuditService(
            List.of("出售枪支"),
            List.of("加微信")
    );

    @Test
    void shouldApproveNormalBlogContent() {
        BlogAutoAuditService.AuditDecision decision = service.audit(
                "Spring Boot 多模块项目实践",
                "记录项目拆分与依赖管理经验",
                "<p>本文主要介绍如何在一个常规业务系统里拆分模块、管理公共依赖，以及如何组织接口分层。</p>"
        );

        assertEquals("published", decision.blogStatus());
        assertEquals("APPROVED", decision.auditStatus());
        assertFalse(decision.requiresManualReview());
    }

    @Test
    void shouldPendingWhenManualReviewKeywordMatched() {
        BlogAutoAuditService.AuditDecision decision = service.audit(
                "领取资料",
                "需要的同学可以继续交流",
                "<p>这篇文章里留一个说明，需要进一步了解可以加微信获取完整资料。</p>"
        );

        assertEquals("pending", decision.blogStatus());
        assertEquals("PENDING", decision.auditStatus());
        assertTrue(decision.requiresManualReview());
        assertTrue(decision.auditReason().contains("人工复核"));
    }

    @Test
    void shouldRejectWhenHighRiskKeywordMatched() {
        BlogAutoAuditService.AuditDecision decision = service.audit(
                "危险内容",
                "测试高风险词命中",
                "<p>这是一段不应通过的文本，里面包含出售枪支等高风险内容。</p>"
        );

        assertEquals("rejected", decision.blogStatus());
        assertEquals("REJECTED", decision.auditStatus());
        assertFalse(decision.requiresManualReview());
        assertTrue(decision.auditReason().contains("高风险词"));
    }
}
