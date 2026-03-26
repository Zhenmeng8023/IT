package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class WithdrawRequestCreateRequest {

    private Long creatorId;
    private Long settlementAccountId;
    private BigDecimal amount;
    private List<WithdrawItemRequest> items;
}
