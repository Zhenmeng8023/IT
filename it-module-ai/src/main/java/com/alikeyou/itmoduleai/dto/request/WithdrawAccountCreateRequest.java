package com.alikeyou.itmoduleai.dto.request;

import com.alikeyou.itmoduleai.entity.CreatorSettlementAccount;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class WithdrawAccountCreateRequest {

    private Long creatorId;
    private CreatorSettlementAccount.AccountType accountType;
    private String accountName;
    private String accountNoMasked;
    private String accountNoEncrypted;
    private String bankName;
    private Boolean isDefault;
}
