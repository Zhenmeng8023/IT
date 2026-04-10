package com.alikeyou.itmodulepayment.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.AlipayApiException;
import com.alikeyou.itmodulecommon.entity.UserInfo;
import com.alikeyou.itmodulecommon.repository.UserInfoRepository;
import com.alikeyou.itmodulepayment.dto.CreatorWithdrawRequestDTO;
import com.alikeyou.itmodulepayment.entity.*;
import com.alikeyou.itmodulepayment.repository.*;
import com.alikeyou.itmodulepayment.service.CreatorWithdrawRequestService;
import com.alikeyou.itmodulepayment.util.PayUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class CreatorWithdrawRequestServiceImpl implements CreatorWithdrawRequestService {

    private static final Logger logger = LoggerFactory.getLogger(CreatorWithdrawRequestServiceImpl.class);

    private final CreatorWithdrawRequestRepository creatorWithdrawRequestRepository;
    private final CreatorSettlementAccountRepository settlementAccountRepository;
    private final UserInfoRepository userInfoRepository;
    private final RevenueRecordRepository revenueRecordRepository;
    private final PayUtil payUtil;

    // 最低提现金额
    private static final BigDecimal MIN_WITHDRAW_AMOUNT = new BigDecimal("10.00");
    // 手续费（目前为0）
    private static final BigDecimal SERVICE_FEE = BigDecimal.ZERO;

    public CreatorWithdrawRequestServiceImpl(CreatorWithdrawRequestRepository creatorWithdrawRequestRepository,
                                             CreatorSettlementAccountRepository settlementAccountRepository,
                                             UserInfoRepository userInfoRepository,
                                             RevenueRecordRepository revenueRecordRepository,
                                             PayUtil payUtil) {
        this.creatorWithdrawRequestRepository = creatorWithdrawRequestRepository;
        this.settlementAccountRepository = settlementAccountRepository;
        this.userInfoRepository = userInfoRepository;
        this.revenueRecordRepository = revenueRecordRepository;
        this.payUtil = payUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorWithdrawRequest submitWithdrawRequest(Long userId, Long settlementAccountId, 
                                                         BigDecimal withdrawAmount, String remark) {
        logger.info("用户 {} 提交提现申请，金额: {}", userId, withdrawAmount);

        // 1. 参数校验
        if (withdrawAmount == null || withdrawAmount.compareTo(MIN_WITHDRAW_AMOUNT) < 0) {
            throw new IllegalArgumentException("提现金额不能低于" + MIN_WITHDRAW_AMOUNT + "元");
        }

        // 2. 验证用户存在
        UserInfo user = userInfoRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        // 3. 验证结算账户存在且属于该用户
        CreatorSettlementAccount account = settlementAccountRepository.findById(settlementAccountId)
                .orElseThrow(() -> new RuntimeException("结算账户不存在"));
        if (!account.getUserId().equals(userId)) {
            throw new RuntimeException("结算账户不属于当前用户");
        }
        if (!"ACTIVE".equals(account.getStatus())) {
            throw new RuntimeException("结算账户未激活");
        }

        // 4. 检查可提现余额
        BigDecimal availableBalance = getAvailableWithdrawBalance(userId);
        if (withdrawAmount.compareTo(availableBalance) > 0) {
            throw new RuntimeException("可提现余额不足，当前可用: " + availableBalance);
        }

        // 5. 生成提现单号
        String requestNo = generateWithdrawNo();

        // 6. 计算实际到账金额
        BigDecimal actualAmount = withdrawAmount.subtract(SERVICE_FEE);

        // 7. 创建提现请求
        CreatorWithdrawRequest request = new CreatorWithdrawRequest();
        request.setRequestNo(requestNo);
        request.setUserId(userId);
        request.setSettlementAccountId(settlementAccountId);
        request.setWithdrawAmount(withdrawAmount);
        request.setServiceFee(SERVICE_FEE);
        request.setActualAmount(actualAmount);
        request.setStatus("PENDING"); // 待审核
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        CreatorWithdrawRequest savedRequest = creatorWithdrawRequestRepository.save(request);
        logger.info("提现申请创建成功，单号: {}", requestNo);

        return savedRequest;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorWithdrawRequest approveWithdrawRequest(Long id, Long reviewedBy, String reviewNote) {
        logger.info("审核通过提现申请，ID: {}, 审核人: {}", id, reviewedBy);

        CreatorWithdrawRequest request = creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("只有待审核状态的申请才能审核");
        }

        request.setStatus("APPROVED");
        request.setReviewedBy(reviewedBy);
        request.setReviewNote(reviewNote);
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        return creatorWithdrawRequestRepository.save(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorWithdrawRequest rejectWithdrawRequest(Long id, Long reviewedBy, String reviewNote) {
        logger.info("拒绝提现申请，ID: {}, 审核人: {}", id, reviewedBy);

        CreatorWithdrawRequest request = creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new RuntimeException("只有待审核状态的申请才能拒绝");
        }

        request.setStatus("REJECTED");
        request.setReviewedBy(reviewedBy);
        request.setReviewNote(reviewNote);
        request.setReviewedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());

        return creatorWithdrawRequestRepository.save(request);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public CreatorWithdrawRequest processWithdrawPayment(Long id) {
        logger.info("开始执行提现打款，ID: {}", id);

        CreatorWithdrawRequest request = creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现申请不存在"));

        if (!"APPROVED".equals(request.getStatus())) {
            throw new RuntimeException("只有审核通过的申请才能打款");
        }

        // 获取结算账户信息
        CreatorSettlementAccount account = settlementAccountRepository.findById(request.getSettlementAccountId())
                .orElseThrow(() -> new RuntimeException("结算账户不存在"));

        try {
            // 调用支付宝转账API（沙箱环境）
            String result = payUtil.transferToAlipay(
                    request.getRequestNo(),
                    account.getAccountNumber(), 
                    account.getAccountName(), // 传入账户持有人姓名
                    request.getActualAmount(),
                    "创作者提现-" + request.getRequestNo()
            );

            // 解析响应获取交易号
            JSONObject responseJson = JSONObject.parseObject(result);
            JSONObject alipayResponse = responseJson.getJSONObject("alipay_fund_trans_uni_transfer_response");
            String tradeNo = alipayResponse.getString("trade_no");

            // 更新提现状态为已打款
            request.setStatus("PAID");
            request.setPayChannelRef(tradeNo);
            request.setPaidAt(LocalDateTime.now());
            request.setUpdatedAt(LocalDateTime.now());

            CreatorWithdrawRequest updatedRequest = creatorWithdrawRequestRepository.save(request);
            logger.info("提现打款成功，单号: {}, 支付宝交易号: {}", request.getRequestNo(), tradeNo);

            return updatedRequest;

        } catch (AlipayApiException e) {
            logger.error("支付宝转账失败，单号: {}", request.getRequestNo(), e);
            throw new RuntimeException("支付宝转账失败: " + e.getErrMsg());
        }
    }

    @Override
    public BigDecimal getAvailableWithdrawBalance(Long userId) {
        // 查询用户所有已结算但未提现的收益记录
        // 注意：数据库中 settlement_status 的值为 'settled'（小写）
        List<RevenueRecord> settledRecords = revenueRecordRepository
                .findBySourceUserIdAndSettlementStatus(userId, "settled");

        // 计算总金额
        BigDecimal totalBalance = settledRecords.stream()
                .map(RevenueRecord::getAuthorRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 减去已提现的金额
        List<CreatorWithdrawRequest> paidRequests = creatorWithdrawRequestRepository
                .findByUserIdAndStatus(userId, "PAID");
        BigDecimal withdrawnAmount = paidRequests.stream()
                .map(CreatorWithdrawRequest::getWithdrawAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal availableBalance = totalBalance.subtract(withdrawnAmount);
        return availableBalance.max(BigDecimal.ZERO); // 确保不为负数
    }

    /**
     * 生成提现单号
     * 格式：WD + 时间戳 + UUID前8位
     */
    private String generateWithdrawNo() {
        return "WD" + System.currentTimeMillis() + 
               UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @Override
    public CreatorWithdrawRequest getCreatorWithdrawRequestById(Long id) {
        return creatorWithdrawRequestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("提现请求不存在"));
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserId(Long userId) {
        return creatorWithdrawRequestRepository.findByUserId(userId);
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByStatus(String status) {
        return creatorWithdrawRequestRepository.findByStatus(status);
    }

    @Override
    public List<CreatorWithdrawRequest> getCreatorWithdrawRequestsByUserIdAndStatus(Long userId, String status) {
        return creatorWithdrawRequestRepository.findByUserIdAndStatus(userId, status);
    }
}