package com.time_tracker.be.balanceHistory.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.UUID;

@Data
public class MidtransRequestDto {
    @JsonProperty("fraud_status")
    private String fraudStatus;
    @JsonProperty("transaction_status")
    private String transactionStatus;
    @JsonProperty("order_id")
    private UUID orderId;
    @JsonProperty("gross_amount")
    private String grossAmount;
    @JsonProperty("payment_type")
    private String paymentType;
    @JsonProperty("transaction_id")
    private String transactionId;
    @JsonProperty("status_code")
    private String statusCode;
    @JsonProperty("signature_key")
    private String signatureKey;

}
