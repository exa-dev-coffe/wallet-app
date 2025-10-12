package com.wallet_service.be.utils.commons;

import lombok.Data;

@Data
public class MidtransResponseDto {
    private String redirectUrl;
    private String token;

    public MidtransResponseDto(String redirectUrl, String token) {
        this.redirectUrl = redirectUrl;
        this.token = token;
    }
}
