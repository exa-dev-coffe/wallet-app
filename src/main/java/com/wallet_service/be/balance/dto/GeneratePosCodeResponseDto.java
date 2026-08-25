package com.wallet_service.be.balance.dto;

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
public class GeneratePosCodeResponseDto {
    private String paymentCode;
    private int expiresInSeconds;
    private double currentBalance;
    private String userName;
    private String userEmail;
}
