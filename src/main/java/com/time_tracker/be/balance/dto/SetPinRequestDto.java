package com.time_tracker.be.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPinRequestDto {

    @NotBlank(message = "PIN is required")
    @Size(min = 6, max = 6, message = "PIN should be exactly 6 digits long")
    @Pattern(regexp = "\\d+", message = "PIN should contain only digits")
    private String pin;
}
