package com.wallet_service.be.internal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PosWalletRefundRequestDto {
    @NotNull(message = "User ID is required")
    private Integer userId;

    @NotNull(message = "Amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Double amount;

    private Long orderId;
}
