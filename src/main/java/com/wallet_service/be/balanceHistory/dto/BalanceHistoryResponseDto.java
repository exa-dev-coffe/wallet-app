package com.wallet_service.be.balanceHistory.dto;

import com.wallet_service.be.balanceHistory.BalancehistoryModel;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.UUID;

@Builder
@Data
public class BalanceHistoryResponseDto {
    private String type;
    private double amount;
    private UUID id;
    private String status;
    private String token;
    private String redirect_url;
    private Date created_at;

    // Static method mapper
    public static BalanceHistoryResponseDto fromEntity(BalancehistoryModel entity) {
        return BalanceHistoryResponseDto.builder()
                .type(entity.getType().getValue()) // enum ke string
                .amount(entity.getAmount())
                .id(entity.getId())
                .status(entity.getStatus().getValue())
                .token(entity.getToken())
                .redirect_url(entity.getRedirectUrl())
                .created_at(entity.getCreatedAt())
                .build();
    }
}
