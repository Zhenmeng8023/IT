package com.alikeyou.itmodulepayment.dto;

import java.util.List;

/**
 * 支付宝支付请求DTO
 */
public class PayRequest {
    private List<Long> orderIds;
    private String value;

    public List<Long> getOrderIds() {
        return orderIds;
    }

    public void setOrderIds(List<Long> orderIds) {
        this.orderIds = orderIds;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "PayRequest{" +
                "orderIds=" + orderIds +
                ", value='" + value + '\'' +
                '}';
    }
}
