package com.time_tracker.be.balance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.format.annotation.NumberFormat;

@Data
public class SetPinRequestDto {

    @NotBlank(message = "PIN is required")
    @NumberFormat(style = NumberFormat.Style.NUMBER)
    @Size(min = 6, max = 6, message = "PIN should be exactly 6 digits long")
    private String pin;
}
