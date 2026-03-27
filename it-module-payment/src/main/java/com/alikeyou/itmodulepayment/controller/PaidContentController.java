package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.PaidContentDTO;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.service.PaidContentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/paid-contents")
public class PaidContentController {

    private final PaidContentService paidContentService;

    public PaidContentController(PaidContentService paidContentService) {
        this.paidContentService = paidContentService;
    }

    // 创建付费内容
    @PostMapping
    public ResponseEntity<PaidContent> createPaidContent(@RequestBody PaidContentDTO dto) {
        PaidContent paidContent = paidContentService.createPaidContent(dto);
        return new ResponseEntity<>(paidContent, HttpStatus.CREATED);
    }

    // 更新付费内容
    @PutMapping("/{id}")
    public ResponseEntity<PaidContent> updatePaidContent(@PathVariable Long id, @RequestBody PaidContentDTO dto) {
        PaidContent paidContent = paidContentService.updatePaidContent(id, dto);
        return new ResponseEntity<>(paidContent, HttpStatus.OK);
    }

    // 删除付费内容
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deletePaidContent(@PathVariable Long id) {
        paidContentService.deletePaidContent(id);
        return new ResponseEntity<>(HttpStatus.NO_CONTENT);
    }

    // 根据ID查询付费内容
    @GetMapping("/{id}")
    public ResponseEntity<PaidContent> getPaidContentById(@PathVariable Long id) {
        PaidContent paidContent = paidContentService.getPaidContentById(id);
        return new ResponseEntity<>(paidContent, HttpStatus.OK);
    }

    // 根据内容类型和内容ID查询付费内容
    @GetMapping("/content/{contentType}/{contentId}")
    public ResponseEntity<PaidContent> getPaidContentByContentTypeAndContentId(
            @PathVariable String contentType, @PathVariable Long contentId) {
        PaidContent paidContent = paidContentService.getPaidContentByContentTypeAndContentId(contentType, contentId);
        return new ResponseEntity<>(paidContent, HttpStatus.OK);
    }

    // 查询所有付费内容
    @GetMapping
    public ResponseEntity<List<PaidContent>> getAllPaidContents() {
        List<PaidContent> paidContents = paidContentService.getAllPaidContents();
        return new ResponseEntity<>(paidContents, HttpStatus.OK);
    }

    // 根据状态查询付费内容
    @GetMapping("/status/{status}")
    public ResponseEntity<List<PaidContent>> getPaidContentsByStatus(@PathVariable String status) {
        List<PaidContent> paidContents = paidContentService.getPaidContentsByStatus(status);
        return new ResponseEntity<>(paidContents, HttpStatus.OK);
    }

    // 根据访问类型查询付费内容
    @GetMapping("/access-type/{accessType}")
    public ResponseEntity<List<PaidContent>> getPaidContentsByAccessType(@PathVariable String accessType) {
        List<PaidContent> paidContents = paidContentService.getPaidContentsByAccessType(accessType);
        return new ResponseEntity<>(paidContents, HttpStatus.OK);
    }

    // 根据创建者查询付费内容
    @GetMapping("/created-by/{createdBy}")
    public ResponseEntity<List<PaidContent>> getPaidContentsByCreatedBy(@PathVariable Long createdBy) {
        List<PaidContent> paidContents = paidContentService.getPaidContentsByCreatedBy(createdBy);
        return new ResponseEntity<>(paidContents, HttpStatus.OK);
    }
}