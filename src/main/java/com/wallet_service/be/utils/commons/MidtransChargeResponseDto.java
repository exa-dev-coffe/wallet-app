package com.wallet_service.be.utils.commons;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MidtransChargeResponseDto {
    private UUID orderId;
    private Double grossAmount;
    private String paymentType;
    private String transactionStatus;
    private String transactionId;
    private String statusCode;
    private String statusMessage;
    private String bank;
    private String vaNumber;
    private String billKey;
    private String billerCode;
    private String qrUrl;
    private String qrString;
    private String deeplinkUrl;
    private String expiryTime;
}
