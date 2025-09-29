package com.time_tracker.be.balanceHistory.dto;

import com.time_tracker.be.balanceHistory.BalancehistoryModel;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
public class BalanceHistoryResponseDto {
    private String type;
    private double amount;
    private String status;
    private String token;
    private String redirect_url;

    // Static method mapper
    public static BalanceHistoryResponseDto fromEntity(BalancehistoryModel entity) {
        return BalanceHistoryResponseDto.builder()
                .type(entity.getType().getValue()) // enum ke string
                .amount(entity.getAmount())
                .status(entity.getStatus().getValue())
                .token(entity.getToken())
                .redirect_url(entity.getRedirectUrl())
                .build();
    }
}
