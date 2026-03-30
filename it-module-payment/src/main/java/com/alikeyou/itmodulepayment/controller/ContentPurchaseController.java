package com.alikeyou.itmodulepayment.controller;

import com.alikeyou.itmodulepayment.dto.ContentPurchaseRequest;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseResponse;
import com.alikeyou.itmodulepayment.service.ContentPurchaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/content-purchase")
public class ContentPurchaseController {

    @Autowired
    private ContentPurchaseService purchaseService;

    /**
     * 检查是否已购买
     */
    @GetMapping("/check/{blogId}")
    public ResponseEntity<Boolean> checkPurchaseStatus(
            @PathVariable Long blogId,
            @RequestHeader("X-User-Id") Long userId) {
        boolean hasPurchased = purchaseService.hasPurchasedBlog(userId, blogId);
        return ResponseEntity.ok(hasPurchased);
    }

    /**
     * 创建购买订单
     */
    @PostMapping("/create-order")
    public ResponseEntity<ContentPurchaseResponse> createOrder(
            @RequestBody ContentPurchaseRequest request,
            @RequestHeader("X-User-Id") Long userId) {
        ContentPurchaseResponse response = purchaseService.createPurchaseOrder(request, userId);
        return ResponseEntity.ok(response);
    }

    /**
     * 确认支付
     */
    @PostMapping("/complete")
    public ResponseEntity<Void> completePurchase(
            @RequestParam Long blogId,
            @RequestParam String orderNo,
            @RequestHeader("X-User-Id") Long userId) {
        // 这里需要通过 orderNo 查找订单 ID
        // TODO: 实现通过 orderNo 查找订单的逻辑
        purchaseService.completePurchase(userId, null, orderNo);
        return ResponseEntity.ok().build();
    }
}
