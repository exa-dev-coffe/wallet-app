package com.time_tracker.be.balance.dto;

import lombok.Data;

@Data
public class BalanceResponseDto {
    private boolean isActive;
    private double balance;
}
