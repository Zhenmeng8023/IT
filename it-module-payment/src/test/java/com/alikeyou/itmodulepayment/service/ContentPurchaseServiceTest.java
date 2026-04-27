package com.alikeyou.itmodulepayment.service;

import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.entity.PaidContent;
import com.alikeyou.itmodulepayment.entity.PaymentOrder;
import com.alikeyou.itmodulepayment.entity.RevenueRecord;
import com.alikeyou.itmodulepayment.entity.UserCoupon;
import com.alikeyou.itmodulepayment.entity.UserPurchase;
import com.alikeyou.itmodulepayment.repository.CouponRepository;
import com.alikeyou.itmodulepayment.repository.MembershipLevelRepository;
import com.alikeyou.itmodulepayment.repository.MembershipRepository;
import com.alikeyou.itmodulepayment.repository.PaidContentRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import com.alikeyou.itmodulepayment.repository.UserCouponRepository;
import com.alikeyou.itmodulepayment.repository.UserPurchaseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentPurchaseServiceTest {

    @Mock
    private PaidContentRepository paidContentRepository;

    @Mock
    private PaymentOrderRepository paymentOrderRepository;

    @Mock
    private RevenueRecordRepository revenueRecordRepository;

    @Mock
    private UserInfoRepository userInfoRepository;

    @Mock
    private MembershipRepository membershipRepository;

    @Mock
    private MembershipLevelRepository membershipLevelRepository;

    @Mock
    private UserPurchaseRepository userPurchaseRepository;

    @Mock
    private UserCouponRepository userCouponRepository;

    @Mock
    private UserCouponService userCouponService;

    @Mock
    private CouponRedemptionService redemptionService;

    @Mock
    private CouponRepository couponRepository;

    @InjectMocks
    private ContentPurchaseService contentPurchaseService;

    @Test
    void completePurchaseFinalizesPendingOrderAndCreatesRevenue() {
        PaymentOrder order = new PaymentOrder();
        order.setId(11L);
        order.setOrderNo("CP202604270001");
        order.setUserId(7L);
        order.setPaidContentId(99L);
        order.setType("content");
        order.setAmount(new BigDecimal("50.00"));
        order.setStatus("pending");

        PaidContent paidContent = new PaidContent();
        paidContent.setId(99L);
        paidContent.setCreatedBy(88L);

        UserInfo author = new UserInfo();
        author.setId(88L);
        author.setBalance(new BigDecimal("12.50"));

        when(paymentOrderRepository.findById(11L)).thenReturn(Optional.of(order));
        when(paidContentRepository.findById(99L)).thenReturn(Optional.of(paidContent));
        when(userPurchaseRepository.findByUserIdAndPaidContentId(7L, 99L)).thenReturn(Optional.empty());
        when(revenueRecordRepository.findByOrderId(11L)).thenReturn(null);
        when(userCouponRepository.findByOrderId(11L)).thenReturn(Optional.empty());
        when(userInfoRepository.findById(88L)).thenReturn(Optional.of(author));

        contentPurchaseService.completePurchase(7L, 11L, null);

        assertEquals("paid", order.getStatus());
        assertNotNull(order.getPayTime());
        verify(paymentOrderRepository).save(order);
        verify(userPurchaseRepository).save(any(UserPurchase.class));
        verify(revenueRecordRepository).save(any(RevenueRecord.class));
    }

    @Test
    void completePurchaseIsIdempotentWhenOrderIsAlreadyPaid() {
        PaymentOrder order = new PaymentOrder();
        order.setId(12L);
        order.setOrderNo("CP202604270002");
        order.setUserId(7L);
        order.setPaidContentId(99L);
        order.setType("content");
        order.setAmount(new BigDecimal("50.00"));
        order.setStatus("paid");
        order.setPayTime(LocalDateTime.now());

        PaidContent paidContent = new PaidContent();
        paidContent.setId(99L);
        paidContent.setCreatedBy(88L);

        RevenueRecord existingRevenue = new RevenueRecord();
        existingRevenue.setId(21L);
        existingRevenue.setOrderId(12L);

        when(paymentOrderRepository.findById(12L)).thenReturn(Optional.of(order));
        when(paidContentRepository.findById(99L)).thenReturn(Optional.of(paidContent));
        when(userPurchaseRepository.findByUserIdAndPaidContentId(7L, 99L)).thenReturn(Optional.of(new UserPurchase()));
        when(revenueRecordRepository.findByOrderId(12L)).thenReturn(existingRevenue);
        when(userCouponRepository.findByOrderId(12L)).thenReturn(Optional.empty());

        contentPurchaseService.completePurchase(7L, 12L, null);

        assertEquals("paid", order.getStatus());
        verify(paymentOrderRepository, never()).save(any());
        verify(revenueRecordRepository, never()).save(any(RevenueRecord.class));
        verify(userPurchaseRepository).save(any(UserPurchase.class));
    }
}
