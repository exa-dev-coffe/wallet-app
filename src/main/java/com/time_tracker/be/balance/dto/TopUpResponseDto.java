package com.time_tracker.be.balance.dto;

import lombok.Data;

@Data
public class TopUpResponseDto {
    private String redirectUrl;
    private String token;

    public TopUpResponseDto(String redirectUrl, String token) {
        this.redirectUrl = redirectUrl;
        this.token = token;
    }
}
