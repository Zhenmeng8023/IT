package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.PaidContentDTO;
import com.alikeyou.itmodulepayment.entity.PaidContent;

import java.util.List;

public interface PaidContentService {

    // 创建付费内容
    PaidContent createPaidContent(PaidContentDTO dto);

    // 更新付费内容
    PaidContent updatePaidContent(Long id, PaidContentDTO dto);

    // 删除付费内容
    void deletePaidContent(Long id);

    // 根据ID查询付费内容
    PaidContent getPaidContentById(Long id);

    // 根据内容类型和内容ID查询付费内容
    PaidContent getPaidContentByContentTypeAndContentId(String contentType, Long contentId);

    // 查询所有付费内容
    List<PaidContent> getAllPaidContents();

    // 根据状态查询付费内容
    List<PaidContent> getPaidContentsByStatus(String status);

    // 根据访问类型查询付费内容
    List<PaidContent> getPaidContentsByAccessType(String accessType);

    // 根据创建者查询付费内容
    List<PaidContent> getPaidContentsByCreatedBy(Long createdBy);
}