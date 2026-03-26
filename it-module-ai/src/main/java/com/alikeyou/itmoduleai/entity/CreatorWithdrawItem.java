package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "creator_withdraw_item", schema = "it9_data")
public class CreatorWithdrawItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "withdraw_request_id", nullable = false)
    private CreatorWithdrawRequest withdrawRequest;

    @Column(name = "revenue_record_id", nullable = false)
    private Long revenueRecordId;

    @Column(name = "amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;

    @Column(name = "created_at")
    private Instant createdAt;
}
