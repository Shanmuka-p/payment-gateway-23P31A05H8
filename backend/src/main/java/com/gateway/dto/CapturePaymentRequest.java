package com.gateway.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CapturePaymentRequest {
    @NotNull
    @Min(1)
    private Long amount;

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
}
