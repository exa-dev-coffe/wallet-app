package com.wallet_service.be.balance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopUpRequestDto {
    @NotNull(message = "Amount is required")
    @Min(value = 1000, message = "Minimum top-up amount is Rp 1.000")
    private Double amount;

    private String paymentType; // "qris", "bank_transfer", "echannel", "gopay", "shopeepay"

    private String bank; // "bca", "bni", "bri", "mandiri", "permata", "cimb"
}

