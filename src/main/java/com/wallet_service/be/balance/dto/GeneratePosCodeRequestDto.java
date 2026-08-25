package com.wallet_service.be.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GeneratePosCodeRequestDto {
    @NotBlank(message = "PIN is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "PIN must be a 6-digit numeric string")
    private String pin;
}
