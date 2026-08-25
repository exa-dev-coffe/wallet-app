package com.wallet_service.be.internal.dto;

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
public class PosWalletPayResponseDto {
    private boolean success;
    private int userId;
    private String customerName;
    private String customerEmail;
    private double amountPaid;
    private double remainingBalance;
    private String message;
}
