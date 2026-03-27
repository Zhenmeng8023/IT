package com.alikeyou.itmodulepayment.service.impl;

import com.alikeyou.itmodulepayment.dto.PaidContentDTO;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.service.PaidContentService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class PaidContentServiceImpl implements PaidContentService {

    private final PaidContentRepository paidContentRepository;

    public PaidContentServiceImpl(PaidContentRepository paidContentRepository) {
        this.paidContentRepository = paidContentRepository;
    }

    @Override
    public PaidContent createPaidContent(PaidContentDTO dto) {
        PaidContent paidContent = new PaidContent();
        BeanUtils.copyProperties(dto, paidContent);
        paidContent.setCreatedAt(LocalDateTime.now());
        paidContent.setUpdatedAt(LocalDateTime.now());
        return paidContentRepository.save(paidContent);
    }

    @Override
    public PaidContent updatePaidContent(Long id, PaidContentDTO dto) {
        PaidContent paidContent = paidContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("付费内容不存在"));
        BeanUtils.copyProperties(dto, paidContent);
        paidContent.setUpdatedAt(LocalDateTime.now());
        return paidContentRepository.save(paidContent);
    }

    @Override
    public void deletePaidContent(Long id) {
        paidContentRepository.deleteById(id);
    }

    @Override
    public PaidContent getPaidContentById(Long id) {
        return paidContentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("付费内容不存在"));
    }

    @Override
    public PaidContent getPaidContentByContentTypeAndContentId(String contentType, Long contentId) {
        return paidContentRepository.findByContentTypeAndContentId(contentType, contentId);
    }

    @Override
    public List<PaidContent> getAllPaidContents() {
        return paidContentRepository.findAll();
    }

    @Override
    public List<PaidContent> getPaidContentsByStatus(String status) {
        return paidContentRepository.findByStatus(status);
    }

    @Override
    public List<PaidContent> getPaidContentsByAccessType(String accessType) {
        return paidContentRepository.findByAccessType(accessType);
    }

    @Override
    public List<PaidContent> getPaidContentsByCreatedBy(Long createdBy) {
        return paidContentRepository.findByCreatedBy(createdBy);
    }
}