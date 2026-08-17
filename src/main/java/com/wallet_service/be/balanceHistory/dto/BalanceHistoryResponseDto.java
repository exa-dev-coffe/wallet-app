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
    private String redirectUrl;
    private String paymentType;
    private String bank;
    private String vaNumber;
    private String billKey;
    private String billerCode;
    private String qrUrl;
    private String qrString;
    private String deeplinkUrl;
    private String expiryTime;
    private String userEmail;
    private String userName;
    private Date createdAt;

    // Static method mapper
    public static BalanceHistoryResponseDto fromEntity(BalancehistoryModel entity) {
        return BalanceHistoryResponseDto.builder()
                .type(entity.getType().getValue()) // enum ke string
                .amount(entity.getAmount())
                .id(entity.getId())
                .status(entity.getStatus().getValue())
                .token(entity.getToken())
                .redirectUrl(entity.getRedirectUrl())
                .paymentType(entity.getPaymentType())
                .bank(entity.getBank())
                .vaNumber(entity.getVaNumber())
                .billKey(entity.getBillKey())
                .billerCode(entity.getBillerCode())
                .qrUrl(entity.getQrUrl())
                .qrString(entity.getQrString())
                .deeplinkUrl(entity.getDeeplinkUrl())
                .expiryTime(entity.getExpiryTime())
                .userEmail(entity.getUserEmail())
                .userName(entity.getUserName())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}


