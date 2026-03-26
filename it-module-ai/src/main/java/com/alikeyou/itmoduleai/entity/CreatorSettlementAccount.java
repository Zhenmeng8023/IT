package com.alikeyou.itmoduleai.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "creator_settlement_account", schema = "it9_data")
public class CreatorSettlementAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "creator_id", nullable = false)
    private Long creatorId;

    @Enumerated(EnumType.STRING)
    @Column(name = "account_type", nullable = false, length = 20)
    private AccountType accountType;

    @Column(name = "account_name", nullable = false, length = 100)
    private String accountName;

    @Column(name = "account_no_masked", nullable = false, length = 100)
    private String accountNoMasked;

    @Column(name = "account_no_encrypted", nullable = false, length = 500)
    private String accountNoEncrypted;

    @Column(name = "bank_name", length = 100)
    private String bankName;

    @Column(name = "is_default")
    private Boolean isDefault;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status;

    @Column(name = "created_at")
    private Instant createdAt;

    @Column(name = "updated_at")
    private Instant updatedAt;

    public enum AccountType {
        ALIPAY, WECHAT, BANK_CARD
    }

    public enum Status {
        ACTIVE, DISABLED
    }
}
