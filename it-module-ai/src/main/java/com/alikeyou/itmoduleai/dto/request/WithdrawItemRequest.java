package com.alikeyou.itmoduleai.dto.request;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class WithdrawItemRequest {

    private Long revenueRecordId;
    private BigDecimal amount;
}
