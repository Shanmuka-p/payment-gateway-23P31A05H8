package com.gateway.dto;

import jakarta.validation.constraints.NotBlank;

public class CreatePaymentRequest {
    @NotBlank
    private String orderId;
    @NotBlank
    private String method;
    private String vpa;

    public String getOrderId() { return orderId; }
    public void setOrderId(String orderId) { this.orderId = orderId; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getVpa() { return vpa; }
    public void setVpa(String vpa) { this.vpa = vpa; }
}
