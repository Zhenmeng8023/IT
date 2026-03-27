package com.alikeyou.itmodulepayment.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "creator_withdraw_item")
public class CreatorWithdrawItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "withdraw_request_id", nullable = false)
    private Long withdrawRequestId;

    @Column(name = "revenue_record_id", nullable = false)
    private Long revenueRecordId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getWithdrawRequestId() {
        return withdrawRequestId;
    }

    public void setWithdrawRequestId(Long withdrawRequestId) {
        this.withdrawRequestId = withdrawRequestId;
    }

    public Long getRevenueRecordId() {
        return revenueRecordId;
    }

    public void setRevenueRecordId(Long revenueRecordId) {
        this.revenueRecordId = revenueRecordId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}