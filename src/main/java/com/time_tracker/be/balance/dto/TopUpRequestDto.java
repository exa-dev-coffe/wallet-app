package com.time_tracker.be.balance.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class TopUpRequestDto {
    @NotNull(message = "Amount is required")
    @Min(value = 10000, message = "Minimum top-up amount is 10,000")
    private Double amount;
    @NotBlank(message = "PIN is required")
    @Size(min = 6, max = 6, message = "PIN should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "PIN should contain only digits")
    private String pin;
}
