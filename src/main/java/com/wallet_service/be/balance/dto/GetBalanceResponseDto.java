package com.wallet_service.be.balance.dto;

import lombok.Data;

@Data
public class GetBalanceResponseDto {
    private Boolean isActive;
    private Double balance;
}
