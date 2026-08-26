package com.wallet_service.be.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePinRequestDto {

    @NotBlank(message = "Old PIN is required")
    @Size(min = 6, max = 6, message = "Old PIN should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "Old PIN should contain only digits")
    private String oldPin;

    @NotBlank(message = "New PIN is required")
    @Size(min = 6, max = 6, message = "New PIN should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "New PIN should contain only digits")
    private String newPin;
}
