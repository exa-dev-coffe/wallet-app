package com.wallet_service.be.internal.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PosQrisChargeRequestDto {
    @NotBlank(message = "Order ID is required")
    private String orderId;

    @NotNull(message = "Gross amount is required")
    @Min(value = 1, message = "Amount must be greater than 0")
    private Double grossAmount;

    private String customerName;
    private String customerEmail;
}
