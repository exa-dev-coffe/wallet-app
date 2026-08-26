package com.wallet_service.be.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ResetPinRequestDto {

    @NotBlank(message = "Verification code is required")
    @Size(min = 6, max = 6, message = "Verification code should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "Verification code should contain only digits")
    private String code;

    @NotBlank(message = "New PIN is required")
    @Size(min = 6, max = 6, message = "New PIN should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "New PIN should contain only digits")
    private String newPin;
}
