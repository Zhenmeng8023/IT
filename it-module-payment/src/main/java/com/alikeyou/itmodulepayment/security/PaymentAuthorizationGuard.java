package com.alikeyou.itmodulepayment.security;

import com.alikeyou.itmodulecommon.constant.LoginConstant;
import com.alikeyou.itmodulecommon.security.AuthorizationGuard;
import com.alikeyou.itmodulepayment.repository.CreatorSettlementAccountRepository;
import com.alikeyou.itmodulepayment.repository.CreatorWithdrawRequestRepository;
import com.alikeyou.itmodulepayment.repository.PaymentOrderRepository;
import com.alikeyou.itmodulepayment.repository.RevenueRecordRepository;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.Objects;

@Component("paymentAuthorizationGuard")
public class PaymentAuthorizationGuard {

    private final AuthorizationGuard authorizationGuard;
    private final PaymentOrderRepository paymentOrderRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final CreatorWithdrawRequestRepository creatorWithdrawRequestRepository;
    private final CreatorSettlementAccountRepository creatorSettlementAccountRepository;

    public PaymentAuthorizationGuard(AuthorizationGuard authorizationGuard,
                                     PaymentOrderRepository paymentOrderRepository,
                                     RevenueRecordRepository revenueRecordRepository,
                                     CreatorWithdrawRequestRepository creatorWithdrawRequestRepository,
                                     CreatorSettlementAccountRepository creatorSettlementAccountRepository) {
        this.authorizationGuard = authorizationGuard;
        this.paymentOrderRepository = paymentOrderRepository;
        this.revenueRecordRepository = revenueRecordRepository;
        this.creatorWithdrawRequestRepository = creatorWithdrawRequestRepository;
        this.creatorSettlementAccountRepository = creatorSettlementAccountRepository;
    }

    public boolean canAccessUser(Object userIdValue) {
        Long userId = toLong(userIdValue);
        return userId != null && authorizationGuard.canAccessUser(userId);
    }

    public boolean canAccessPaymentOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && paymentOrderRepository.findById(orderId)
                .map(order -> Objects.equals(order.getUserId(), currentUserId))
                .orElse(false);
    }

    public boolean canAccessPaymentOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && paymentOrderRepository.findByOrderNo(orderNo.trim())
                .map(order -> Objects.equals(order.getUserId(), currentUserId))
                .orElse(false);
    }

    public boolean canAccessRevenueRecord(Long recordId) {
        if (recordId == null) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && revenueRecordRepository.findById(recordId)
                .map(record -> Objects.equals(record.getSourceUserId(), currentUserId))
                .orElse(false);
    }

    public boolean canAccessRevenueRecordByOrder(Long orderId) {
        if (orderId == null) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        if (currentUserId == null) {
            return false;
        }
        var revenueRecord = revenueRecordRepository.findByOrderId(orderId);
        return revenueRecord != null && Objects.equals(revenueRecord.getSourceUserId(), currentUserId);
    }

    public boolean canAccessCreatorWithdrawRequest(Long requestId) {
        if (requestId == null) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && creatorWithdrawRequestRepository.findById(requestId)
                .map(request -> Objects.equals(request.getUserId(), currentUserId))
                .orElse(false);
    }

    public boolean canAccessCreatorSettlementAccount(Long accountId) {
        if (accountId == null) {
            return false;
        }
        if (authorizationGuard.canManageUsers()) {
            return true;
        }
        Long currentUserId = LoginConstant.getUserId();
        return currentUserId != null && creatorSettlementAccountRepository.findById(accountId)
                .map(account -> Objects.equals(account.getUserId(), currentUserId))
                .orElse(false);
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ignored) {
                return null;
            }
        }
        return null;
    }
}
