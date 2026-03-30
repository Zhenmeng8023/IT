package com.alikeyou.itmodulepayment.repository;

import com.alikeyou.itmodulepayment.entity.PaidContent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaidContentRepository extends JpaRepository<PaidContent, Long> {

    // 根据博客 ID 查询付费内容
    PaidContent findByBlogId(Long blogId);

    // 根据内容类型和内容 ID 查询付费内容
    PaidContent findByContentTypeAndContentId(String contentType, Long contentId);

    // 根据状态查询付费内容
    List<PaidContent> findByStatus(String status);

    // 根据访问类型查询付费内容
    List<PaidContent> findByAccessType(String accessType);

    // 根据创建者查询付费内容
    List<PaidContent> findByCreatedBy(Long createdBy);
}