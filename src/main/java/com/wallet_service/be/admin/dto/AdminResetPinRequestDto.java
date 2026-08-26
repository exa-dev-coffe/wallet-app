package com.wallet_service.be.admin.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class AdminResetPinRequestDto {
    private Integer userId;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    @NotBlank(message = "Verification code is required")
    private String code;

    @NotBlank(message = "New PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be 6 digits")
    private String newPin;
}
