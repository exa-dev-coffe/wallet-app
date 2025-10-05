package com.time_tracker.be.balance.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class TopUpRequestDto {
    @NotNull(message = "Amount is required")
    @Min(value = 10000, message = "Minimum top-up amount is 10,000")
    private Double amount;
}
