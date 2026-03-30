package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulepayment.dto.ContentPurchaseRequest;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseResponse;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ContentPurchaseService {

    @Autowired
    private PaidContentRepository paidContentRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private RevenueRecordRepository revenueRecordRepository;

    // 注意：这里需要注入 blog 模块的 Repository，暂时用 Object 代替
    // 实际应该使用 @Qualifier 或其他方式注入
    // @Autowired
    // private BlogRepository blogRepository;

    /**
     * 检查用户是否已购买某篇博客
     */
    public boolean hasPurchasedBlog(Long userId, Long blogId) {
        // 1. 查找付费内容记录
        PaidContent paidContent = paidContentRepository.findByBlogId(blogId);
        if (paidContent == null) {
            return true; // 不是付费博客，直接返回已购买
        }

        // 2. 检查是否是一次性购买类型（数据库存储小写）
        if ("one_time".equals(paidContent.getAccessType())) {
            return paymentOrderRepository.existsByUserIdAndPaidContentIdAndStatus(
                userId, paidContent.getId(), "PAID"
            );
        }

        // 3. 如果是会员专属，需要检查用户 VIP 状态（需要注入 UserService）
        // TODO: 实现 VIP 状态检查
        return false;
    }

    /**
     * 创建购买订单
     */
    @Transactional
    public ContentPurchaseResponse createPurchaseOrder(ContentPurchaseRequest request, Long userId) {
        ContentPurchaseResponse response = new ContentPurchaseResponse();

        // 1. 查找付费内容记录
        PaidContent paidContent = paidContentRepository.findByBlogId(request.getBlogId());
        if (paidContent == null) {
            response.setSuccess(false);
            response.setMessage("该博客不是付费博客");
            return response;
        }

        // 2. 检查博客是否为付费博客（price > 0）
        if (paidContent.getPrice() == null || paidContent.getPrice().compareTo(BigDecimal.ZERO) <= 0) {
            response.setSuccess(false);
            response.setMessage("该博客不是付费博客");
            return response;
        }

        // 3. 检查用户是否已购买
        if (hasPurchasedBlog(userId, request.getBlogId())) {
            response.setSuccess(true);
            response.setAlreadyPurchased(true);
            response.setMessage("您已购买过该博客");
            return response;
        }

        // 4. 创建支付订单
        PaymentOrder order = new PaymentOrder();
        String orderNo = "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setType("CONTENT");
        order.setTargetId(paidContent.getId());
        order.setPaidContentId(paidContent.getId());
        order.setAmount(paidContent.getPrice());
        order.setPaymentMethod(request.getPaymentMethod());
        order.setStatus("PENDING");

        paymentOrderRepository.save(order);

        response.setSuccess(true);
        response.setAlreadyPurchased(false);
        response.setOrderNo(orderNo);
        response.setAmount(paidContent.getPrice());
        response.setMessage("订单创建成功");

        return response;
    }

    /**
     * 确认支付并完成购买
     */
    @Transactional
    public void completePurchase(Long userId, Long orderId, String orderNo) {
        // 1. 查找订单
        PaymentOrder order = paymentOrderRepository.findById(orderId)
            .orElseThrow(() -> new RuntimeException("订单不存在"));

        // 2. 更新订单状态
        order.setStatus("PAID");
        order.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(order);

        // 3. 获取付费内容信息
        PaidContent paidContent = paidContentRepository.findById(order.getPaidContentId())
            .orElseThrow(() -> new RuntimeException("付费内容不存在"));

        // 4. 创建收益记录（平台抽成 20%，作者获得 80%）
        BigDecimal platformFee = order.getAmount().multiply(new BigDecimal("0.2"));
        BigDecimal authorRevenue = order.getAmount().subtract(platformFee);

        RevenueRecord revenueRecord = new RevenueRecord();
        revenueRecord.setOrderId(orderId);
        // revenueRecord.setSourceUserId(blog.getAuthorId()); // 需要从 Blog 获取作者 ID
        revenueRecord.setPlatformRevenue(platformFee);
        revenueRecord.setAuthorRevenue(authorRevenue);
        revenueRecord.setSettlementStatus("UNSETTLED");

        revenueRecordRepository.save(revenueRecord);

        // 5. TODO: 更新作者余额（需要注入 BlogRepository 和 UserRepository）
    }
}
