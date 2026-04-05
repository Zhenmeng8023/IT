package com.alikeyou.itmodulepayment.dto;

/**
 * 支付宝退款请求DTO
 */
public class RefundRequest {
    private Long orderId;
    private String payId;
    private String value;
    private String reason;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getPayId() {
        return payId;
    }

    public void setPayId(String payId) {
        this.payId = payId;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    @Override
    public String toString() {
        return "RefundRequest{" +
                "orderId=" + orderId +
                ", payId='" + payId + '\'' +
                ", value='" + value + '\'' +
                ", reason='" + reason + '\'' +
                '}';
    }
}
