package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseRequest;
import com.alikeyou.itmodulepayment.dto.ContentPurchaseResponse;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ContentPurchaseService {

    private static final Logger logger = LoggerFactory.getLogger(ContentPurchaseService.class);

    @Autowired
    private PaidContentRepository paidContentRepository;

    @Autowired
    private PaymentOrderRepository paymentOrderRepository;

    @Autowired
    private RevenueRecordRepository revenueRecordRepository;

    @Autowired
    private UserInfoRepository userInfoRepository;

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
                userId, paidContent.getId(), "paid"
            );
        }

        // 3. 如果是会员专属，检查用户 VIP 状态
        if ("membership".equals(paidContent.getAccessType())) {
            UserInfo user = userInfoRepository.findById(userId).orElse(null);
            if (user != null && Boolean.TRUE.equals(user.getIsPremiumMember())) {
                // 检查 VIP 是否过期
                if (user.getPremiumExpiryDate() != null) {
                    LocalDateTime now = LocalDateTime.now();
                    LocalDateTime expiryTime = LocalDateTime.ofInstant(
                        user.getPremiumExpiryDate(), 
                        java.time.ZoneId.systemDefault()
                    );
                    return expiryTime.isAfter(now);
                }
                return false;
            }
            return false;
        }

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

        // 4. 验证支付方式（只支持微信和支付宝）
        String paymentMethod = request.getPaymentMethod();
        if (!"wechat".equals(paymentMethod) && !"alipay".equals(paymentMethod)) {
            response.setSuccess(false);
            response.setMessage("不支持的支付方式，请使用微信支付或支付宝支付");
            return response;
        }

        // 5. 创建支付订单
        PaymentOrder order = new PaymentOrder();
        String orderNo = "CP" + System.currentTimeMillis() + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setType("CONTENT");
        order.setTargetId(paidContent.getId());
        order.setPaidContentId(paidContent.getId());
        order.setAmount(paidContent.getPrice());
        order.setPaymentMethod(paymentMethod);
        order.setStatus("pending");

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
        PaymentOrder order;
        if (orderId != null) {
            order = paymentOrderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        } else if (orderNo != null) {
            order = paymentOrderRepository.findByOrderNo(orderNo)
                .orElseThrow(() -> new RuntimeException("订单不存在"));
        } else {
            throw new RuntimeException("订单ID和订单号不能同时为空");
        }

        // 2. 验证订单状态（忽略大小写）
        if (!"pending".equalsIgnoreCase(order.getStatus())) {
            throw new RuntimeException("订单状态不正确，当前状态：" + order.getStatus());
        }

        // 3. 验证订单归属
        if (!order.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作此订单");
        }

        // 4. 更新订单状态
        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());
        paymentOrderRepository.save(order);

        // 5. 获取付费内容信息
        PaidContent paidContent = paidContentRepository.findById(order.getPaidContentId())
            .orElseThrow(() -> new RuntimeException("付费内容不存在"));

        // 6. 获取博客信息以获取作者 ID
        // 注意：这里需要通过 blogId 找到作者，但由于跨模块依赖，暂时使用 paidContent 的 createdBy
        Long authorId = paidContent.getCreatedBy();
        if (authorId == null) {
            logger.warn("付费内容缺少作者ID，无法分配收益，paidContentId: {}", paidContent.getId());
        }

        // 7. 创建收益记录（平台抽成 20%，作者获得 80%）
        BigDecimal platformFee = order.getAmount().multiply(new BigDecimal("0.2"));
        BigDecimal authorRevenue = order.getAmount().subtract(platformFee);

        RevenueRecord revenueRecord = new RevenueRecord();
        revenueRecord.setOrderId(order.getId());
        revenueRecord.setSourceUserId(authorId);
        revenueRecord.setPlatformRevenue(platformFee);
        revenueRecord.setAuthorRevenue(authorRevenue);
        revenueRecord.setSettlementStatus("UNSETTLED");
        revenueRecord.setCreatedAt(LocalDateTime.now());
        revenueRecord.setUpdatedAt(LocalDateTime.now());

        revenueRecordRepository.save(revenueRecord);

        // 8. 更新作者余额
        if (authorId != null) {
            UserInfo author = userInfoRepository.findById(authorId).orElse(null);
            if (author != null) {
                BigDecimal currentBalance = author.getBalance() != null ? author.getBalance() : BigDecimal.ZERO;
                author.setBalance(currentBalance.add(authorRevenue));
                userInfoRepository.save(author);
                logger.info("作者余额更新成功，作者ID: {}, 新增收益: {}, 当前余额: {}", 
                    authorId, authorRevenue, author.getBalance());
            } else {
                logger.warn("作者用户不存在，无法更新余额，作者ID: {}", authorId);
            }
        }

        logger.info("购买完成，订单号: {}, 用户ID: {}, 金额: {}", order.getOrderNo(), userId, order.getAmount());
    }
}
