package com.time_tracker.be.internal.dto;

import lombok.Data;

@Data
public class PaymentRequestDto {
    private Integer userId;
    private Double amount;
    private String pin;
}
